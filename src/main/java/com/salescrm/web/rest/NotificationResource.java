package com.salescrm.web.rest;

import com.salescrm.domain.CrmNotification;
import com.salescrm.repository.UserRepository;
import com.salescrm.security.SecurityUtils;
import com.salescrm.service.CrmNotificationService;
import com.salescrm.service.FirebasePushNotificationService;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing internal enterprise mailbox notifications.
 */
@RestController
@RequestMapping("/api/notifications")
public class NotificationResource {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationResource.class);

    private final CrmNotificationService crmNotificationService;
    private final FirebasePushNotificationService firebasePushNotificationService;
    private final UserRepository userRepository;

    public NotificationResource(
        CrmNotificationService crmNotificationService,
        @Autowired(required = false) FirebasePushNotificationService firebasePushNotificationService,
        UserRepository userRepository
    ) {
        this.firebasePushNotificationService = firebasePushNotificationService;
        this.crmNotificationService = crmNotificationService;
        this.userRepository = userRepository;
    }

    /**
     * {@code POST  /api/notifications/fcm-token} : Register device FCM token for push notifications.
     */
    @PostMapping("/fcm-token")
    public Mono<ResponseEntity<Map<String, Object>>> registerFcmToken(@RequestBody Map<String, String> payload) {
        String token = payload != null ? payload.get("token") : null;
        String requestedUserId = payload != null ? payload.get("userId") : null;
        String numericId = payload != null ? payload.get("numericId") : null;

        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty(requestedUserId != null && !requestedUserId.isBlank() ? requestedUserId : "admin")
            .flatMap(login -> {
                if (firebasePushNotificationService != null && token != null) {
                    firebasePushNotificationService.registerToken(login, token);
                    if (requestedUserId != null) {
                        firebasePushNotificationService.registerToken(requestedUserId, token);
                    }
                    if (numericId != null) {
                        firebasePushNotificationService.registerToken(numericId, token);
                    }
                }
                return userRepository.findOneByLogin(login)
                    .doOnNext(user -> {
                        if (firebasePushNotificationService != null && token != null && user.getId() != null) {
                            firebasePushNotificationService.registerToken(user.getId().toString(), token);
                        }
                    })
                    .thenReturn(login);
            })
            .map(userId -> {
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("userId", userId);
                return ResponseEntity.ok(response);
            });
    }

    private Mono<String> resolveUserId(String requestedUserId) {
        if (requestedUserId != null && !requestedUserId.isBlank()) {
            return Mono.just(requestedUserId);
        }
        return SecurityUtils.getCurrentUserLogin().defaultIfEmpty("admin");
    }

    /**
     * {@code GET  /api/notifications} : Internal notification inbox.
     */
    @GetMapping("")
    public Mono<ResponseEntity<List<CrmNotification>>> getNotifications(
        @RequestParam(name = "userId", required = false) String userId,
        @RequestParam(name = "unreadOnly", required = false) Boolean unreadOnly
    ) {
        LOG.debug("REST request to get notifications. userId: {}, unreadOnly: {}", userId, unreadOnly);
        return resolveUserId(userId)
            .flatMap(resolvedId -> crmNotificationService.getNotifications(resolvedId, unreadOnly).collectList())
            .map(ResponseEntity::ok);
    }

    /**
     * {@code GET  /api/notifications/unread-count} : Real-time unread badge counter.
     */
    @GetMapping("/unread-count")
    public Mono<ResponseEntity<Map<String, Object>>> getUnreadCount(
        @RequestParam(name = "userId", required = false) String userId
    ) {
        LOG.debug("REST request to count unread notifications for userId: {}", userId);
        return resolveUserId(userId)
            .flatMap(crmNotificationService::getUnreadCount)
            .map(count -> {
                Map<String, Object> response = new HashMap<>();
                response.put("unreadCount", count);
                response.put("count", count);
                return ResponseEntity.ok(response);
            });
    }

    /**
     * {@code POST  /api/notifications} : Send a new internal notification.
     */
    @PostMapping("")
    public Mono<ResponseEntity<CrmNotification>> createNotification(@Valid @RequestBody CrmNotification notification) {
        LOG.debug("REST request to create CrmNotification : {}", notification);
        Mono<String> userResolver = (notification.getUserId() != null && !notification.getUserId().isBlank())
            ? Mono.just(notification.getUserId())
            : SecurityUtils.getCurrentUserLogin().defaultIfEmpty("admin");

        return userResolver.flatMap(userId -> {
            notification.setUserId(userId);
            return crmNotificationService
                .createNotification(notification)
                .map(result -> {
                    try {
                        if (firebasePushNotificationService != null) { firebasePushNotificationService.sendPushNotification(result.getUserId(), result.getTitle(), result.getMessage(), null); }
                        return ResponseEntity.created(new URI("/api/notifications/" + result.getId())).body(result);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
        });
    }

    /**
     * {@code PATCH  /api/notifications/:id/read} : Mark a single notification as read.
     */
    @PatchMapping("/{id}/read")
    public Mono<ResponseEntity<CrmNotification>> markAsRead(@PathVariable String id) {
        LOG.debug("REST request to mark notification {} as read", id);
        if (id == null || id.isBlank()) {
            return Mono.just(ResponseEntity.noContent().build());
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            LOG.warn("Non-UUID notification id received for markAsRead: {}. Returning noContent.", id);
            return Mono.just(ResponseEntity.noContent().build());
        }
        return crmNotificationService
            .markAsRead(uuid)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    public Mono<ResponseEntity<CrmNotification>> markAsRead(UUID id) {
        return markAsRead(id != null ? id.toString() : null);
    }

    /**
     * {@code PATCH  /api/notifications/read-all} : Mark all notifications for current user as read.
     */
    @PatchMapping("/read-all")
    public Mono<ResponseEntity<Map<String, Object>>> markAllAsRead(
        @RequestParam(name = "userId", required = false) String userId
    ) {
        LOG.debug("REST request to mark all notifications as read for userId: {}", userId);
        return resolveUserId(userId)
            .flatMap(crmNotificationService::markAllAsRead)
            .map(updatedCount -> {
                Map<String, Object> result = new HashMap<>();
                result.put("updatedCount", updatedCount);
                result.put("success", true);
                return ResponseEntity.ok(result);
            });
    }

    /**
     * {@code DELETE  /api/notifications/:id} : Delete a single notification.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteNotification(@PathVariable String id) {
        LOG.debug("REST request to delete notification: {}", id);
        if (id == null || id.isBlank()) {
            return Mono.just(ResponseEntity.noContent().build());
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            LOG.warn("Non-UUID notification id received for deleteNotification: {}. Returning noContent.", id);
            return Mono.just(ResponseEntity.noContent().build());
        }
        return crmNotificationService.deleteNotification(uuid).thenReturn(ResponseEntity.noContent().build());
    }

    public Mono<ResponseEntity<Void>> deleteNotification(UUID id) {
        return deleteNotification(id != null ? id.toString() : null);
    }

    /**
     * {@code DELETE  /api/notifications} : Clear inbox items.
     */
    @DeleteMapping("")
    public Mono<ResponseEntity<Void>> clearNotifications(
        @RequestParam(name = "userId", required = false) String userId
    ) {
        LOG.debug("REST request to clear notifications for userId: {}", userId);
        return resolveUserId(userId)
            .flatMap(crmNotificationService::clearAllNotifications)
            .thenReturn(ResponseEntity.noContent().build());
    }
}
