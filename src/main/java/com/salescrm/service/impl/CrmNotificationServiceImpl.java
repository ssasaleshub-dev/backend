package com.salescrm.service.impl;

import com.salescrm.domain.CrmNotification;
import com.salescrm.domain.User;
import com.salescrm.repository.CrmNotificationRepository;
import com.salescrm.repository.UserRepository;
import com.salescrm.service.CrmNotificationService;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/**
 * Service Implementation for managing {@link CrmNotification}.
 */
@Service
@Transactional
public class CrmNotificationServiceImpl implements CrmNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(CrmNotificationServiceImpl.class);

    private final CrmNotificationRepository crmNotificationRepository;
    private final UserRepository userRepository;
    private final Sinks.Many<CrmNotification> notificationSink = Sinks.many().multicast().onBackpressureBuffer();

    public CrmNotificationServiceImpl(CrmNotificationRepository crmNotificationRepository, UserRepository userRepository) {
        this.crmNotificationRepository = crmNotificationRepository;
        this.userRepository = userRepository;
    }

    private Mono<String> resolveAltUserId(String userId) {
        if (userId == null || userId.isBlank() || userRepository == null) {
            return Mono.just("");
        }
        try {
            Long numericId = Long.parseLong(userId.trim());
            return userRepository.findById(numericId)
                .map(User::getLogin)
                .defaultIfEmpty(userId);
        } catch (NumberFormatException e) {
            return userRepository.findOneByLogin(userId.trim().toLowerCase())
                .map(u -> u.getId() != null ? u.getId().toString() : userId)
                .defaultIfEmpty(userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<CrmNotification> getNotifications(String userId, Boolean unreadOnly) {
        LOG.debug("Request to get notifications for user: {}, unreadOnly: {}", userId, unreadOnly);
        final String cleanUserId = (userId != null && !userId.isBlank()) ? userId.trim() : "admin";
        return resolveAltUserId(cleanUserId)
            .flatMapMany(altUserId -> {
                if (Boolean.TRUE.equals(unreadOnly)) {
                    return crmNotificationRepository.findAllByUserIdOrAltUserIdAndIsReadFalseOrderByCreatedAtDesc(cleanUserId, altUserId);
                }
                return crmNotificationRepository.findAllByUserIdOrAltUserIdOrderByCreatedAtDesc(cleanUserId, altUserId);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Long> getUnreadCount(String userId) {
        LOG.debug("Request to count unread notifications for user: {}", userId);
        final String cleanUserId = (userId != null && !userId.isBlank()) ? userId.trim() : "admin";
        return resolveAltUserId(cleanUserId)
            .flatMap(altUserId -> crmNotificationRepository.countByUserIdOrAltUserIdAndIsReadFalse(cleanUserId, altUserId));
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
        final String cleanUserId = (userId != null && !userId.isBlank()) ? userId.trim() : "admin";
        return resolveAltUserId(cleanUserId)
            .flatMap(altUserId -> crmNotificationRepository.markAllAsReadForUserOrAltUserId(cleanUserId, altUserId, Instant.now()));
    }

    @Override
    public Mono<Void> deleteNotification(UUID id) {
        LOG.debug("Request to delete notification: {}", id);
        return crmNotificationRepository.deleteById(id);
    }

    @Override
    public Mono<Long> clearAllNotifications(String userId) {
        LOG.debug("Request to clear all notifications for user: {}", userId);
        final String cleanUserId = (userId != null && !userId.isBlank()) ? userId.trim() : "admin";
        return resolveAltUserId(cleanUserId)
            .flatMap(altUserId -> crmNotificationRepository.deleteAllByUserIdOrAltUserId(cleanUserId, altUserId));
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

        boolean isChat = "CHAT".equalsIgnoreCase(notification.getType());
        boolean hasRefKey =
            !isChat &&
            notification.getUserId() != null &&
            !notification.getUserId().isBlank() &&
            notification.getReferenceId() != null &&
            !notification.getReferenceId().isBlank() &&
            notification.getType() != null &&
            !notification.getType().isBlank();

        Mono<CrmNotification> persistAndBroadcast = crmNotificationRepository
            .save(notification)
            .map(CrmNotification::setIsPersisted)
            .doOnSuccess(saved -> {
                if (saved != null) {
                    broadcastNotification(saved);
                }
            });

        if (hasRefKey) {
            return crmNotificationRepository
                .findLatestByUserIdIgnoreCaseAndReferenceIdAndType(
                    notification.getUserId().trim(),
                    notification.getReferenceId().trim(),
                    notification.getType().trim()
                )
                .map(CrmNotification::setIsPersisted)
                .switchIfEmpty(persistAndBroadcast);
        }

        return persistAndBroadcast;
    }

    @Override
    public Flux<CrmNotification> getLiveNotificationStream() {
        return notificationSink.asFlux();
    }

    @Override
    public void broadcastNotification(CrmNotification notification) {
        if (notification != null) {
            LOG.info("[LiveNotification] Broadcasting real-time notification id={} to user={}: '{}'",
                notification.getId(), notification.getUserId(), notification.getTitle());
            notificationSink.tryEmitNext(notification);
        }
    }
}
