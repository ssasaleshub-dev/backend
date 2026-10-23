package com.salescrm.service.impl;

import com.salescrm.domain.CrmNotification;
import com.salescrm.repository.CrmNotificationRepository;
import com.salescrm.service.CrmNotificationService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link CrmNotification}.
 */
@Service
@Transactional
public class CrmNotificationServiceImpl implements CrmNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(CrmNotificationServiceImpl.class);

    private final CrmNotificationRepository crmNotificationRepository;

    public CrmNotificationServiceImpl(CrmNotificationRepository crmNotificationRepository) {
        this.crmNotificationRepository = crmNotificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<CrmNotification> getNotifications(String userId, Boolean unreadOnly) {
        LOG.debug("Request to get notifications for user: {}, unreadOnly: {}", userId, unreadOnly);
        if (Boolean.TRUE.equals(unreadOnly)) {
            return crmNotificationRepository.findAllByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        }
        return crmNotificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Long> getUnreadCount(String userId) {
        LOG.debug("Request to count unread notifications for user: {}", userId);
        return crmNotificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Override
    public Mono<CrmNotification> markAsRead(UUID id) {
        LOG.debug("Request to mark notification as read: {}", id);
        return crmNotificationRepository
            .findById(id)
            .flatMap(existing -> {
                existing.setIsPersisted();
                existing.setIsRead(true);
                existing.setReadAt(Instant.now());
                return crmNotificationRepository.save(existing);
            })
            .map(CrmNotification::setIsPersisted);
    }

    @Override
    public Mono<Long> markAllAsRead(String userId) {
        LOG.debug("Request to mark all notifications as read for user: {}", userId);
        return crmNotificationRepository.markAllAsRead(userId, Instant.now());
    }

    @Override
    public Mono<Void> deleteNotification(UUID id) {
        LOG.debug("Request to delete notification: {}", id);
        return crmNotificationRepository.deleteById(id);
    }

    @Override
    public Mono<Long> clearAllNotifications(String userId) {
        LOG.debug("Request to clear all notifications for user: {}", userId);
        return crmNotificationRepository.deleteAllByUserId(userId);
    }

    @Override
    public Mono<CrmNotification> createNotification(CrmNotification notification) {
        LOG.debug("Request to create notification: {}", notification);
        if (notification.getId() == null) {
            notification.setId(UUID.randomUUID());
        }
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(Instant.now());
        }
        if (notification.getIsRead() == null) {
            notification.setIsRead(false);
        }
        return crmNotificationRepository.save(notification).map(CrmNotification::setIsPersisted);
    }
}
