package com.salescrm.service;

import com.salescrm.domain.CrmNotification;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CrmNotificationService {

    Flux<CrmNotification> getNotifications(String userId, Boolean unreadOnly);

    Mono<Long> getUnreadCount(String userId);

    Mono<CrmNotification> markAsRead(UUID id);

    Mono<Long> markAllAsRead(String userId);

    Mono<Void> deleteNotification(UUID id);

    Mono<Long> clearAllNotifications(String userId);

    Mono<CrmNotification> createNotification(CrmNotification notification);
}
