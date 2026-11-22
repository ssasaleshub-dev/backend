package com.salescrm.repository;

import com.salescrm.domain.AttendanceVerification;
import java.time.Instant;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AttendanceVerificationRepository extends ReactiveCrudRepository<AttendanceVerification, Long> {
    Mono<AttendanceVerification> findFirstByUserIdAndStatusOrderByCreatedAtDesc(String userId, String status);

    /** Expires every open challenge for a user (a new request supersedes older ones). */
    @Modifying
    @Query("UPDATE attendance_verification SET status = 'EXPIRED', resolved_at = :now WHERE user_id = :userId AND status = 'PENDING'")
    Mono<Long> expirePendingForUser(String userId, Instant now);

    /**
     * Moves a challenge out of PENDING exactly once. Returns 1 for the caller that wins, 0 if it was
     * already resolved, so a signed challenge can never be used twice.
     */
    @Modifying
    @Query(
        "UPDATE attendance_verification SET status = :status, resolved_at = :now, approved_device_id = :deviceId " +
        "WHERE id = :id AND status = 'PENDING'"
    )
    Mono<Long> resolvePending(Long id, String status, Instant now, String deviceId);
}
