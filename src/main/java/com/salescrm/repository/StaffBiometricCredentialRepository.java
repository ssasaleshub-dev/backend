package com.salescrm.repository;

import com.salescrm.domain.StaffBiometricCredential;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface StaffBiometricCredentialRepository extends ReactiveCrudRepository<StaffBiometricCredential, Long> {

    Flux<StaffBiometricCredential> findAllByStatusOrderByCreatedAtDesc(String status);

    Flux<StaffBiometricCredential> findAllByUserId(String userId);

    Mono<StaffBiometricCredential> findByUserIdAndDeviceId(String userId, String deviceId);

    Mono<StaffBiometricCredential> findByUserIdAndIsLockedTrue(String userId);

    Mono<StaffBiometricCredential> findByBiometricKeyId(String biometricKeyId);

    @Modifying
    @Query("UPDATE staff_biometric_credentials SET status = 'LOCKED', is_locked = true, locked_at = CURRENT_TIMESTAMP, locked_by = :lockedBy, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Long> lockCredential(Long id, String lockedBy);

    @Modifying
    @Query("UPDATE staff_biometric_credentials SET status = 'REVOKED', is_locked = false, updated_at = CURRENT_TIMESTAMP WHERE id = :id")
    Mono<Long> revokeCredential(Long id);
}
