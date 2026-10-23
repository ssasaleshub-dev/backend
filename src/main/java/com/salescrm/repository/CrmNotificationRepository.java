package com.salescrm.repository;

import com.salescrm.domain.CrmNotification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface CrmNotificationRepository extends ReactiveCrudRepository<CrmNotification, UUID> {

    Flux<CrmNotification> findAllByUserIdOrderByCreatedAtDesc(String userId);

    Flux<CrmNotification> findAllByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);

    Mono<Long> countByUserIdAndIsReadFalse(String userId);

    @Modifying
    @Query("UPDATE crm_notifications SET is_read = true, read_at = :readAt WHERE user_id = :userId AND is_read = false")
    Mono<Long> markAllAsRead(String userId, Instant readAt);

    @Modifying
    @Query("DELETE FROM crm_notifications WHERE user_id = :userId")
    Mono<Long> deleteAllByUserId(String userId);
}
