package com.salescrm.service.impl;

import com.salescrm.domain.StaffBiometricCredential;
import com.salescrm.repository.StaffBiometricCredentialRepository;
import com.salescrm.repository.UserRepository;
import com.salescrm.service.StaffBiometricService;
import com.salescrm.service.dto.StaffBiometricDTO;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class StaffBiometricServiceImpl implements StaffBiometricService {

    private static final Logger LOG = LoggerFactory.getLogger(StaffBiometricServiceImpl.class);

    private final StaffBiometricCredentialRepository credentialRepository;
    private final UserRepository userRepository;

    public StaffBiometricServiceImpl(
        StaffBiometricCredentialRepository credentialRepository,
        UserRepository userRepository
    ) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Mono<StaffBiometricDTO> enrollBiometric(StaffBiometricDTO dto) {
        if (dto == null || dto.getUserId() == null || dto.getUserId().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required for biometric registration"));
        }
        if (dto.getDeviceId() == null || dto.getDeviceId().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Device ID is required"));
        }
        if (dto.getBiometricKeyId() == null || dto.getBiometricKeyId().isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Biometric Key ID is required"));
        }

        final String cleanUserId = dto.getUserId().trim().toLowerCase();
        final String cleanDeviceId = dto.getDeviceId().trim();
        final String cleanKeyId = dto.getBiometricKeyId().trim();

        return credentialRepository.findByUserIdAndDeviceId(cleanUserId, cleanDeviceId)
            .flatMap(existing -> {
                existing.setBiometricKeyId(cleanKeyId);
                existing.setDeviceName(dto.getDeviceName());
                existing.setStatus("PENDING_APPROVAL");
                existing.setIsLocked(false);
                existing.setLockedAt(null);
                existing.setLockedBy(null);
                existing.setUpdatedAt(Instant.now());
                LOG.info("Re-enrolled biometric key for user {} on device {}. Status set to PENDING_APPROVAL.", cleanUserId, cleanDeviceId);
                return credentialRepository.save(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
                StaffBiometricCredential credential = new StaffBiometricCredential();
                credential.setUserId(cleanUserId);
                credential.setUserName(dto.getUserName() != null ? dto.getUserName() : cleanUserId);
                credential.setDeviceId(cleanDeviceId);
                credential.setDeviceName(dto.getDeviceName() != null ? dto.getDeviceName() : "Office Device");
                credential.setBiometricKeyId(cleanKeyId);
                credential.setStatus("PENDING_APPROVAL");
                credential.setIsLocked(false);
                credential.setCreatedAt(Instant.now());
                credential.setUpdatedAt(Instant.now());
                LOG.info("Enrolled new biometric key for user {} on device {}. Awaiting Admin lock.", cleanUserId, cleanDeviceId);
                return credentialRepository.save(credential);
            }))
            .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<StaffBiometricDTO> getPendingApprovals() {
        return credentialRepository.findAllByStatusOrderByCreatedAtDesc("PENDING_APPROVAL")
            .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<StaffBiometricDTO> getAllCredentials() {
        return credentialRepository.findAll()
            .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<StaffBiometricDTO> getCredentialsByUser(String userId) {
        if (userId == null || userId.isBlank()) return Flux.empty();
        return credentialRepository.findAllByUserId(userId.trim().toLowerCase())
            .map(this::toDTO);
    }

    @Override
    public Mono<StaffBiometricDTO> lockAndApprove(Long id, String adminLogin) {
        if (id == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credential ID is required"));
        }
        return credentialRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Biometric credential not found")))
            .flatMap(cred -> {
                cred.setStatus("LOCKED");
                cred.setIsLocked(true);
                cred.setLockedAt(Instant.now());
                cred.setLockedBy(adminLogin != null ? adminLogin : "admin");
                cred.setUpdatedAt(Instant.now());
                LOG.info("Admin [{}] successfully approved and locked biometric credential ID={} for user={}",
                    adminLogin, id, cred.getUserId());
                return credentialRepository.save(cred);
            })
            .map(this::toDTO);
    }

    @Override
    public Mono<StaffBiometricDTO> revokeCredential(Long id, String adminLogin) {
        if (id == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Credential ID is required"));
        }
        return credentialRepository.findById(id)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Biometric credential not found")))
            .flatMap(cred -> {
                cred.setStatus("REVOKED");
                cred.setIsLocked(false);
                cred.setUpdatedAt(Instant.now());
                LOG.info("Admin [{}] revoked biometric credential ID={} for user={}", adminLogin, id, cred.getUserId());
                return credentialRepository.save(cred);
            })
            .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Boolean> verifyCheckInBiometric(String userId, String deviceId, String biometricKeyId) {
        if (userId == null || userId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required for verification"));
        }

        final String cleanUser = userId.trim().toLowerCase();

        return credentialRepository.findByUserIdAndIsLockedTrue(cleanUser)
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "No approved biometric credential found for user. Your fingerprint must be approved and locked by Admin on an office device before check-in.")))
            .flatMap(lockedCred -> {
                // 1. Verify Office Device ID match
                if (deviceId == null || !deviceId.trim().equalsIgnoreCase(lockedCred.getDeviceId().trim())) {
                    LOG.warn("SECURITY ALERT: Check-in rejected for user [{}] - Device mismatch! Expected [{}] but got [{}]",
                        cleanUser, lockedCred.getDeviceId(), deviceId);
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Attendance blocked: Check-in is only permitted from the Admin-approved office device."));
                }

                // 2. Verify Hardware Biometric Signature match
                if (biometricKeyId == null || !biometricKeyId.trim().equalsIgnoreCase(lockedCred.getBiometricKeyId().trim())) {
                    LOG.warn("SECURITY ALERT: Check-in rejected for user [{}] - Biometric fingerprint mismatch!", cleanUser);
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Attendance blocked: Fingerprint signature does not match your registered biometric ID."));
                }

                LOG.info("Biometric verification SUCCESSFUL for user [{}] on office device [{}]", cleanUser, deviceId);
                return Mono.just(true);
            });
    }

    private StaffBiometricDTO toDTO(StaffBiometricCredential entity) {
        if (entity == null) return null;
        StaffBiometricDTO dto = new StaffBiometricDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setUserName(entity.getUserName());
        dto.setDeviceId(entity.getDeviceId());
        dto.setDeviceName(entity.getDeviceName());
        dto.setBiometricKeyId(entity.getBiometricKeyId());
        dto.setStatus(entity.getStatus());
        dto.setIsLocked(entity.getIsLocked());
        dto.setLockedAt(entity.getLockedAt());
        dto.setLockedBy(entity.getLockedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
