package com.salescrm.service;

import com.salescrm.domain.AttendanceVerification;
import com.salescrm.domain.StaffBiometricCredential;
import com.salescrm.repository.AttendanceVerificationRepository;
import com.salescrm.repository.StaffBiometricCredentialRepository;
import com.salescrm.service.dto.StaffAttendanceDTO;
import com.salescrm.service.util.AttendanceSignatureVerifier;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * Fingerprint-verified check-in.
 *
 * <ol>
 *   <li>The staff member (on the web or in the app) requests a one-time challenge.</li>
 *   <li>From the web, a push notification asks their registered phone to approve it.</li>
 *   <li>The phone scans the fingerprint, which unlocks a hardware-held private key, and signs the
 *       challenge. The server verifies the signature against the public key registered for that
 *       phone and only then records the check-in.</li>
 * </ol>
 * Without the registered phone and a matching fingerprint there is no valid signature, so a
 * check-in cannot be forged from a browser, a script or another phone.
 */
@Service
public class AttendanceVerificationService {

    private static final Logger LOG = LoggerFactory.getLogger(AttendanceVerificationService.class);

    static final Duration CHALLENGE_TTL = Duration.ofMinutes(2);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AttendanceVerificationRepository verificationRepository;
    private final StaffBiometricCredentialRepository credentialRepository;
    private final StaffAttendanceService staffAttendanceService;
    private final FirebasePushNotificationService pushService;

    public AttendanceVerificationService(
        AttendanceVerificationRepository verificationRepository,
        StaffBiometricCredentialRepository credentialRepository,
        StaffAttendanceService staffAttendanceService,
        @Autowired(required = false) FirebasePushNotificationService pushService
    ) {
        this.verificationRepository = verificationRepository;
        this.credentialRepository = credentialRepository;
        this.staffAttendanceService = staffAttendanceService;
        this.pushService = pushService;
    }

    /** The admin-approved (locked) credential that can sign for this user, newest first. */
    Mono<StaffBiometricCredential> findSigningCredential(String userId) {
        return credentialRepository
            .findAllByUserId(userId)
            .filter(c -> Boolean.TRUE.equals(c.getIsLocked()) && "LOCKED".equalsIgnoreCase(c.getStatus()))
            .sort(Comparator.comparing((StaffBiometricCredential c) -> c.getLockedAt() != null ? c.getLockedAt() : Instant.EPOCH).reversed())
            .next()
            .switchIfEmpty(
                Mono.error(
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "No approved fingerprint registration. Register your fingerprint in the SSA Android app and ask an admin to approve it."
                    )
                )
            )
            .flatMap(c -> {
                if (c.getPublicKey() == null || c.getPublicKey().isBlank()) {
                    return Mono.error(
                        new ResponseStatusException(
                            HttpStatus.FORBIDDEN,
                            "Your fingerprint registration is outdated. Register again in the SSA Android app and ask an admin to approve it."
                        )
                    );
                }
                return Mono.just(c);
            });
    }

    /** Starts a check-in. From the web this also pushes an approval request to the user's phone. */
    public Mono<Map<String, Object>> createCheckInChallenge(String userId, String source, String workMode, String note, String requestedFrom) {
        String user = normalize(userId);
        String origin = "APP".equalsIgnoreCase(source) ? "APP" : "WEB";
        Instant now = Instant.now();

        return findSigningCredential(user)
            .flatMap(credential ->
                verificationRepository
                    .expirePendingForUser(user, now)
                    .then(
                        Mono.defer(() -> {
                            AttendanceVerification v = new AttendanceVerification();
                            v.setUserId(user);
                            v.setAction(AttendanceVerification.ACTION_CHECK_IN);
                            v.setNonce(randomNonce());
                            v.setStatus(AttendanceVerification.STATUS_PENDING);
                            v.setSource(origin);
                            v.setWorkMode(normalizeWorkMode(workMode));
                            v.setNote(truncate(note, 500));
                            v.setRequestedFrom(truncate(requestedFrom, 500));
                            v.setCreatedAt(now);
                            v.setExpiresAt(now.plus(CHALLENGE_TTL));
                            return verificationRepository.save(v);
                        })
                    )
                    .doOnNext(saved -> {
                        if ("WEB".equals(origin)) {
                            notifyPhone(saved, credential);
                        }
                    })
            )
            .map(this::toView);
    }

    /** A challenge as seen by its owner. Pending challenges past their deadline are reported as expired. */
    public Mono<Map<String, Object>> getForOwner(Long id, String userId) {
        return loadOwned(id, userId).flatMap(this::expireIfStale).map(this::toView);
    }

    /** The newest open challenge for the user, if any (lets the app pick up a missed push). */
    public Mono<Map<String, Object>> getPending(String userId) {
        return verificationRepository
            .findFirstByUserIdAndStatusOrderByCreatedAtDesc(normalize(userId), AttendanceVerification.STATUS_PENDING)
            .flatMap(this::expireIfStale)
            .filter(v -> AttendanceVerification.STATUS_PENDING.equals(v.getStatus()))
            .map(this::toView);
    }

    /**
     * Verifies the phone's fingerprint signature and records the check-in.
     * Fails with 403 for a wrong signature/device and 409 for a challenge that is no longer pending.
     */
    public Mono<StaffAttendanceDTO> approve(Long id, String userId, String signature, String deviceId) {
        String user = normalize(userId);
        Instant now = Instant.now();

        return loadOwned(id, user)
            .flatMap(v -> {
                if (!AttendanceVerification.STATUS_PENDING.equals(v.getStatus())) {
                    return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "This check-in request was already " + v.getStatus().toLowerCase() + "."));
                }
                if (v.isExpired(now)) {
                    return verificationRepository
                        .resolvePending(v.getId(), AttendanceVerification.STATUS_EXPIRED, now, null)
                        .then(Mono.error(new ResponseStatusException(HttpStatus.GONE, "This check-in request expired. Please start again.")));
                }
                return findSigningCredential(user).flatMap(credential -> {
                    if (deviceId == null || !deviceId.trim().equalsIgnoreCase(credential.getDeviceId().trim())) {
                        LOG.warn("SECURITY: check-in approval for [{}] came from unregistered device [{}]", user, deviceId);
                        return Mono.error(
                            new ResponseStatusException(HttpStatus.FORBIDDEN, "Approve check-in from the phone your fingerprint is registered on.")
                        );
                    }
                    String message = AttendanceSignatureVerifier.signedMessage(v.getId(), v.getNonce(), user);
                    if (!AttendanceSignatureVerifier.verify(credential.getPublicKey(), message, signature)) {
                        LOG.warn("SECURITY: invalid fingerprint signature for check-in of [{}] (challenge {})", user, v.getId());
                        return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Fingerprint verification failed."));
                    }
                    return verificationRepository
                        .resolvePending(v.getId(), AttendanceVerification.STATUS_APPROVED, now, credential.getDeviceId())
                        .flatMap(updated -> {
                            if (updated == null || updated == 0) {
                                return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "This check-in request was already used."));
                            }
                            String deviceInfo =
                                (credential.getDeviceName() != null ? credential.getDeviceName() : "Registered phone") +
                                " · fingerprint verified" +
                                ("WEB".equals(v.getSource()) ? " (approved for web check-in)" : "");
                            LOG.info("Fingerprint-verified check-in for [{}] via {} (challenge {})", user, v.getSource(), v.getId());
                            return staffAttendanceService.checkIn(
                                user,
                                credential.getUserName(),
                                null,
                                v.getWorkMode(),
                                v.getNote(),
                                null,
                                deviceInfo
                            );
                        });
                });
            });
    }

    /** The owner declines a request (e.g. "this wasn't me"). */
    public Mono<Map<String, Object>> reject(Long id, String userId) {
        return loadOwned(id, userId)
            .flatMap(v ->
                verificationRepository
                    .resolvePending(v.getId(), AttendanceVerification.STATUS_REJECTED, Instant.now(), null)
                    .then(verificationRepository.findById(v.getId()))
            )
            .map(this::toView);
    }

    private Mono<AttendanceVerification> loadOwned(Long id, String userId) {
        String user = normalize(userId);
        return verificationRepository
            .findById(id)
            .filter(v -> user.equals(v.getUserId()))
            // Someone else's challenge looks the same as a missing one
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Check-in request not found.")));
    }

    private Mono<AttendanceVerification> expireIfStale(AttendanceVerification v) {
        if (AttendanceVerification.STATUS_PENDING.equals(v.getStatus()) && v.isExpired(Instant.now())) {
            v.setStatus(AttendanceVerification.STATUS_EXPIRED);
            return verificationRepository.resolvePending(v.getId(), AttendanceVerification.STATUS_EXPIRED, Instant.now(), null).thenReturn(v);
        }
        return Mono.just(v);
    }

    private void notifyPhone(AttendanceVerification v, StaffBiometricCredential credential) {
        if (pushService == null) {
            return;
        }
        Map<String, String> data = new HashMap<>();
        data.put("type", "ATTENDANCE_VERIFY");
        data.put("challengeId", String.valueOf(v.getId()));
        data.put("route", "/attendance/verify/" + v.getId());
        try {
            pushService.sendPushNotification(
                v.getUserId(),
                "Approve your check-in",
                "A check-in was requested from the web. Tap to confirm with your fingerprint.",
                data
            );
        } catch (Exception e) {
            LOG.warn("Could not push check-in approval request to [{}]: {}", v.getUserId(), e.getMessage());
        }
    }

    private Map<String, Object> toView(AttendanceVerification v) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", v.getId());
        view.put("action", v.getAction());
        view.put("status", v.getStatus());
        view.put("source", v.getSource());
        view.put("workMode", v.getWorkMode());
        view.put("requestedFrom", v.getRequestedFrom());
        view.put("createdAt", v.getCreatedAt());
        view.put("expiresAt", v.getExpiresAt());
        // The phone needs the nonce to build the exact message it signs; it is useless without the key
        if (AttendanceVerification.STATUS_PENDING.equals(v.getStatus())) {
            view.put("nonce", v.getNonce());
            view.put("signedMessage", AttendanceSignatureVerifier.signedMessage(v.getId(), v.getNonce(), v.getUserId()));
        }
        return view;
    }

    private static String randomNonce() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String normalize(String userId) {
        return userId == null ? "" : userId.trim().toLowerCase();
    }

    private static String normalizeWorkMode(String workMode) {
        if (workMode == null) return "OFFICE";
        String m = workMode.trim().toUpperCase();
        return switch (m) {
            case "OFFICE", "REMOTE", "FIELD" -> m;
            default -> "OFFICE";
        };
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        String t = s.trim();
        return t.length() <= max ? t : t.substring(0, max);
    }
}
