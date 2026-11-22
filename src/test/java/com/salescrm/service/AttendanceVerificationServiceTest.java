package com.salescrm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.salescrm.domain.AttendanceVerification;
import com.salescrm.domain.StaffBiometricCredential;
import com.salescrm.repository.AttendanceVerificationRepository;
import com.salescrm.repository.StaffBiometricCredentialRepository;
import com.salescrm.service.dto.StaffAttendanceDTO;
import com.salescrm.service.util.AttendanceSignatureVerifier;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceVerificationServiceTest {

    private static final String USER = "ranjani";
    private static final String DEVICE = "android-device-123";

    @Mock
    private AttendanceVerificationRepository verificationRepository;

    @Mock
    private StaffBiometricCredentialRepository credentialRepository;

    @Mock
    private StaffAttendanceService staffAttendanceService;

    private AttendanceVerificationService service;
    private KeyPair phoneKey;

    @BeforeEach
    void setUp() throws Exception {
        service = new AttendanceVerificationService(verificationRepository, credentialRepository, staffAttendanceService, null);
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        phoneKey = gen.generateKeyPair();

        StaffBiometricCredential cred = new StaffBiometricCredential();
        cred.setUserId(USER);
        cred.setUserName("Ranjani R");
        cred.setDeviceId(DEVICE);
        cred.setDeviceName("Pixel 8");
        cred.setStatus("LOCKED");
        cred.setIsLocked(true);
        cred.setLockedAt(Instant.now());
        cred.setPublicKey(Base64.getEncoder().encodeToString(phoneKey.getPublic().getEncoded()));
        when(credentialRepository.findAllByUserId(USER)).thenReturn(Flux.just(cred));

        when(staffAttendanceService.checkIn(eq(USER), any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(new StaffAttendanceDTO()));
    }

    private AttendanceVerification pendingChallenge(Instant expiresAt) {
        AttendanceVerification v = new AttendanceVerification();
        v.setId(42L);
        v.setUserId(USER);
        v.setAction(AttendanceVerification.ACTION_CHECK_IN);
        v.setNonce("nonce-abc");
        v.setStatus(AttendanceVerification.STATUS_PENDING);
        v.setSource("WEB");
        v.setWorkMode("OFFICE");
        v.setCreatedAt(Instant.now());
        v.setExpiresAt(expiresAt);
        when(verificationRepository.findById(42L)).thenReturn(Mono.just(v));
        return v;
    }

    private static String sign(PrivateKey key, String message) throws Exception {
        Signature s = Signature.getInstance("SHA256withECDSA");
        s.initSign(key);
        s.update(message.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(s.sign());
    }

    @Test
    void validFingerprintSignatureRecordsCheckIn() throws Exception {
        pendingChallenge(Instant.now().plusSeconds(60));
        when(verificationRepository.resolvePending(eq(42L), eq("APPROVED"), any(), eq(DEVICE))).thenReturn(Mono.just(1L));

        String signature = sign(phoneKey.getPrivate(), AttendanceSignatureVerifier.signedMessage(42L, "nonce-abc", USER));
        StaffAttendanceDTO result = service.approve(42L, USER, signature, DEVICE).block();

        assertThat(result).isNotNull();
        verify(staffAttendanceService).checkIn(eq(USER), eq("Ranjani R"), any(), eq("OFFICE"), any(), any(), any());
    }

    @Test
    void signatureFromAnotherKeyIsRejected() throws Exception {
        pendingChallenge(Instant.now().plusSeconds(60));
        KeyPairGenerator gen = KeyPairGenerator.getInstance("EC");
        gen.initialize(new ECGenParameterSpec("secp256r1"));
        PrivateKey attackerKey = gen.generateKeyPair().getPrivate();

        String forged = sign(attackerKey, AttendanceSignatureVerifier.signedMessage(42L, "nonce-abc", USER));

        assertThatThrownBy(() -> service.approve(42L, USER, forged, DEVICE).block())
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        verify(staffAttendanceService, never()).checkIn(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void signatureOverDifferentChallengeIsRejected() throws Exception {
        pendingChallenge(Instant.now().plusSeconds(60));
        String replayed = sign(phoneKey.getPrivate(), AttendanceSignatureVerifier.signedMessage(41L, "old-nonce", USER));

        assertThatThrownBy(() -> service.approve(42L, USER, replayed, DEVICE).block()).isInstanceOf(ResponseStatusException.class);
        verify(staffAttendanceService, never()).checkIn(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void approvalFromUnregisteredDeviceIsRejected() throws Exception {
        pendingChallenge(Instant.now().plusSeconds(60));
        String signature = sign(phoneKey.getPrivate(), AttendanceSignatureVerifier.signedMessage(42L, "nonce-abc", USER));

        assertThatThrownBy(() -> service.approve(42L, USER, signature, "some-other-phone").block())
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void expiredChallengeIsRejected() throws Exception {
        pendingChallenge(Instant.now().minusSeconds(1));
        when(verificationRepository.resolvePending(anyLong(), eq("EXPIRED"), any(), any())).thenReturn(Mono.just(1L));
        String signature = sign(phoneKey.getPrivate(), AttendanceSignatureVerifier.signedMessage(42L, "nonce-abc", USER));

        assertThatThrownBy(() -> service.approve(42L, USER, signature, DEVICE).block())
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.GONE));
        verify(staffAttendanceService, never()).checkIn(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void challengeCannotBeUsedTwice() throws Exception {
        pendingChallenge(Instant.now().plusSeconds(60));
        // Another request already moved it out of PENDING between our read and our update
        when(verificationRepository.resolvePending(eq(42L), eq("APPROVED"), any(), any())).thenReturn(Mono.just(0L));
        String signature = sign(phoneKey.getPrivate(), AttendanceSignatureVerifier.signedMessage(42L, "nonce-abc", USER));

        assertThatThrownBy(() -> service.approve(42L, USER, signature, DEVICE).block())
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(staffAttendanceService, never()).checkIn(anyString(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void otherUsersCannotApproveSomeoneElsesChallenge() throws Exception {
        pendingChallenge(Instant.now().plusSeconds(60));
        String signature = sign(phoneKey.getPrivate(), AttendanceSignatureVerifier.signedMessage(42L, "nonce-abc", USER));

        assertThatThrownBy(() -> service.approve(42L, "someone-else", signature, DEVICE).block())
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void userWithoutRegisteredKeyCannotStartCheckIn() {
        StaffBiometricCredential legacy = new StaffBiometricCredential();
        legacy.setUserId("old-user");
        legacy.setDeviceId(DEVICE);
        legacy.setStatus("LOCKED");
        legacy.setIsLocked(true);
        when(credentialRepository.findAllByUserId("old-user")).thenReturn(Flux.just(legacy));

        assertThatThrownBy(() -> service.createCheckInChallenge("old-user", "WEB", "OFFICE", null, "Chrome").block())
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void startingCheckInReturnsMessageForPhoneToSign() {
        when(verificationRepository.expirePendingForUser(eq(USER), any())).thenReturn(Mono.just(0L));
        when(verificationRepository.save(any(AttendanceVerification.class))).thenAnswer(inv -> {
            AttendanceVerification v = inv.getArgument(0);
            v.setId(7L);
            return Mono.just(v);
        });

        Map<String, Object> view = service.createCheckInChallenge(USER, "APP", "remote", "hello", "Android").block();

        assertThat(view).isNotNull();
        assertThat(view.get("status")).isEqualTo("PENDING");
        assertThat(view.get("workMode")).isEqualTo("REMOTE");
        assertThat((String) view.get("signedMessage")).startsWith("SSA-ATTENDANCE|v1|7|").endsWith("|ranjani");
    }
}
