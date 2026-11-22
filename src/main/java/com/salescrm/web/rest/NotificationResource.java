package com.salescrm.web.rest;

import com.salescrm.domain.CrmNotification;
import com.salescrm.repository.CrmNotificationRepository;
import com.salescrm.repository.UserRepository;
import com.salescrm.security.LeadAccessScope;
import com.salescrm.security.SecurityUtils;
import com.salescrm.service.CrmNotificationService;
import com.salescrm.service.FirebasePushNotificationService;
import com.salescrm.service.LeadAccessService;
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
import org.springframework.security.access.AccessDeniedException;
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
    private final CrmNotificationRepository crmNotificationRepository;
    private final LeadAccessService leadAccessService;

    @Autowired
    public NotificationResource(
        CrmNotificationService crmNotificationService,
        @Autowired(required = false) FirebasePushNotificationService firebasePushNotificationService,
        UserRepository userRepository,
        CrmNotificationRepository crmNotificationRepository,
        LeadAccessService leadAccessService
    ) {
        this.firebasePushNotificationService = firebasePushNotificationService;
        this.crmNotificationService = crmNotificationService;
        this.userRepository = userRepository;
        this.crmNotificationRepository = crmNotificationRepository;
        this.leadAccessService = leadAccessService;
    }

    /** The current user's numeric id as a string ("" when unknown). Notifications may be stored under it. */
    private Mono<String> currentNumericUserId(LeadAccessScope scope) {
        if (scope.getLogin().isEmpty() || userRepository == null) {
            return Mono.just("");
        }
        return userRepository
            .findOneByLogin(scope.getLogin().toLowerCase())
            .map(u -> u.getId() != null ? u.getId().toString() : "")
            .defaultIfEmpty("");
    }

    /** Admins may act on any notification; everyone else only on their own. */
    private Mono<Void> checkNotificationOwnership(UUID id) {
        return leadAccessService.currentScope().flatMap(scope -> {
            if (scope.isAdmin()) {
                return Mono.<Void>empty();
            }
            return crmNotificationRepository
                .findById(id)
                .flatMap(notification ->
                    currentNumericUserId(scope).flatMap(numericId -> {
                        String owner = notification.getUserId() != null ? notification.getUserId().trim() : "";
                        boolean mine = owner.equalsIgnoreCase(scope.getLogin()) || (!numericId.isEmpty() && owner.equals(numericId));
                        return mine ? Mono.<Void>empty() : Mono.<Void>error(new AccessDeniedException("Not your notification"));
                    })
                );
        });
    }

    /**
     * {@code POST  /api/notifications/fcm-token} : Register device FCM token for push notifications.
     */
    @PostMapping("/fcm-token")
    public Mono<ResponseEntity<Map<String, Object>>> registerFcmToken(@RequestBody Map<String, String> payload) {
        String token = payload != null ? payload.get("token") : null;
        String requestedUserId = payload != null ? payload.get("userId") : null;
        String numericId = payload != null ? payload.get("numericId") : null;
        String deviceType = payload != null ? (payload.get("deviceType") != null ? payload.get("deviceType") : payload.get("platform")) : null;

        // A device is only ever registered for the authenticated user. The userId / numericId sent
        // by the client are ignored: honouring them would let anyone receive another user's pushes.
        if (requestedUserId != null || numericId != null) {
            LOG.debug("Ignoring client-supplied user identifiers on FCM token registration");
        }
        return SecurityUtils.getCurrentUserLogin()
            .switchIfEmpty(Mono.error(new AccessDeniedException("Login required to register a device")))
            .flatMap(rawLogin -> {
                String login = rawLogin.trim().toLowerCase();
                if (firebasePushNotificationService != null && token != null) {
                    firebasePushNotificationService.registerToken(login, token, deviceType);
                }
                return userRepository.findOneByLogin(login)
                    .doOnNext(user -> {
                        if (firebasePushNotificationService != null && token != null && user.getId() != null) {
                            firebasePushNotificationService.registerToken(user.getId().toString(), token, deviceType);
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

    /**
     * {@code POST  /api/notifications/test-push} : Send a manual test push notification to a user.
     */
    @PostMapping("/test-push")
    public Mono<ResponseEntity<Map<String, Object>>> sendTestPush(@RequestBody(required = false) Map<String, String> payload) {
        String requestedTarget = payload != null && payload.get("userId") != null ? payload.get("userId") : null;
        String title = payload != null && payload.get("title") != null ? payload.get("title") : "Test Push Notification";
        String body = payload != null && payload.get("body") != null ? payload.get("body") : "Testing push notification delivery";

        return leadAccessService.currentScope().map(scope -> {
            // Admins may test-push anyone; everyone else only themselves
            String targetUser = scope.isAdmin() && requestedTarget != null ? requestedTarget : scope.getLogin();
            if (firebasePushNotificationService != null && !targetUser.isEmpty()) {
                Map<String, String> data = new HashMap<>();
                data.put("type", "TEST");
                data.put("route", "/chat");
                firebasePushNotificationService.sendPushNotification(targetUser, title, body, data);
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("status", "dispatched");
            resp.put("targetUser", targetUser);
            return ResponseEntity.ok(resp);
        });
    }

    /**
     * Whose inbox a request targets. Admins may name any user; everyone else always gets their
     * own inbox (a client-supplied userId is ignored, so one user cannot read another's).
     */
    private Mono<String> resolveUserId(String requestedUserId) {
        return leadAccessService.currentScope().flatMap(scope -> {
            if (scope.isAdmin() && requestedUserId != null && !requestedUserId.isBlank()) {
                return Mono.just(requestedUserId.trim().toLowerCase());
            }
            if (scope.getLogin().isEmpty()) {
                return Mono.error(new AccessDeniedException("Login required"));
            }
            return Mono.just(scope.getLogin().toLowerCase());
        });
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
        String requestedOwner = notification.getUserId();
        Mono<String> userResolver = leadAccessService.currentScope().map(scope -> {
            if (scope.isAdmin() && requestedOwner != null && !requestedOwner.isBlank()) {
                return requestedOwner;
            }
            return scope.getLogin().isEmpty() ? "admin" : scope.getLogin();
        });

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
        return checkNotificationOwnership(uuid)
            .then(Mono.defer(() -> crmNotificationService.markAsRead(uuid)))
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
        return checkNotificationOwnership(uuid)
            .then(Mono.defer(() -> crmNotificationService.deleteNotification(uuid)))
            .thenReturn(ResponseEntity.noContent().build());
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
