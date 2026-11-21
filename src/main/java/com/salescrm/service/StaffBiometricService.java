package com.salescrm.service;

import com.salescrm.service.dto.StaffBiometricDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StaffBiometricService {

    Mono<StaffBiometricDTO> enrollBiometric(StaffBiometricDTO dto);

    Flux<StaffBiometricDTO> getPendingApprovals();

    Flux<StaffBiometricDTO> getAllCredentials();

    Flux<StaffBiometricDTO> getCredentialsByUser(String userId);

    Mono<StaffBiometricDTO> lockAndApprove(Long id, String adminLogin);

    Mono<StaffBiometricDTO> revokeCredential(Long id, String adminLogin);

    /**
     * Verifies that the staff user's check-in punch matches their Admin-Locked biometric credential.
     *
     * @param userId staff user login
     * @param deviceId hardware device ID
     * @param biometricKeyId keystore biometric signature / public key ID
     * @return Mono<Boolean> true if valid, error Mono if verification fails
     */
    Mono<Boolean> verifyCheckInBiometric(String userId, String deviceId, String biometricKeyId);
}
