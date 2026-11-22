package com.salescrm.config.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.domain.CrmNotification;
import com.salescrm.service.CrmNotificationService;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring WebFlux Reactive WebSocket Handler for Live Push Notifications.
 * Exposes /ws/notifications, /api/ws/notifications, /ws/alerts, and /api/ws/alerts.
 * Dispatches real-time Apple HIG notification banner alerts, sound chimes, and live counter updates.
 */
@Component
public class NotificationWebSocketHandler implements WebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationWebSocketHandler.class);

    private final CrmNotificationService crmNotificationService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    private final Map<String, String> sessionUsers = new ConcurrentHashMap<>();
    private final WebSocketAuthenticator authenticator;

    public NotificationWebSocketHandler(
        CrmNotificationService crmNotificationService,
        ObjectMapper objectMapper,
        WebSocketAuthenticator authenticator
    ) {
        this.crmNotificationService = crmNotificationService;
        this.objectMapper = objectMapper;
        this.authenticator = authenticator;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        // Only authenticated users may connect; each receives only their own notifications
        return authenticator
            .authenticate(session)
            .flatMap(user -> handleAuthenticated(session, user))
            .switchIfEmpty(Mono.defer(() -> session.close(CloseStatus.POLICY_VIOLATION.withReason("Authentication required"))));
    }

    private Mono<Void> handleAuthenticated(WebSocketSession session, WebSocketAuthenticator.SocketUser user) {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        sessionUsers.put(sessionId, user.login());
        LOG.info("[NotificationWebSocket] Client connected: sessionId={}, total active={}", sessionId, activeSessions.size());

        // Send initial CONNECTED frame
        Mono<Void> initialHandshake = session.send(
            Mono.fromCallable(() -> {
                Map<String, Object> welcome = Map.of(
                    "event", "CONNECTED",
                    "sessionId", sessionId,
                    "service", "Spring WebFlux Live Notification Service"
                );
                return session.textMessage(objectMapper.writeValueAsString(welcome));
            })
        );

        // Outgoing stream: broadcast live notifications from crmNotificationService
        Flux<WebSocketMessage> outgoingFlux = crmNotificationService.getLiveNotificationStream()
            .filter(notif -> user.isTarget(notif.getUserId()))
            .flatMap(notif -> {
                try {
                    Map<String, Object> frame = createNotificationFrame(notif);
                    String json = objectMapper.writeValueAsString(frame);
                    return Mono.just(session.textMessage(json));
                } catch (Exception e) {
                    LOG.error("[NotificationWebSocket] Error serializing notification frame: {}", e.getMessage());
                    return Mono.empty();
                }
            });

        // Incoming handler: support AUTHENTICATE frame from frontend client
        Mono<Void> incomingHandler = session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .flatMap(payload -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> frame = objectMapper.readValue(payload, Map.class);
                    String event = (String) frame.get("event");
                    if ("AUTHENTICATE".equalsIgnoreCase(event)) {
                        // Identity comes from the verified token; client-claimed logins are ignored
                        LOG.debug("[NotificationWebSocket] Session {} already authenticated as {}", sessionId, user.login());
                    }
                } catch (Exception e) {
                    LOG.debug("[NotificationWebSocket] Frame parse notice: {}", e.getMessage());
                }
                return Mono.empty();
            })
            .then();

        return initialHandshake
            .then(session.send(outgoingFlux).and(incomingHandler))
            .doFinally(signalType -> {
                activeSessions.remove(sessionId);
                sessionUsers.remove(sessionId);
                LOG.info("[NotificationWebSocket] Client disconnected: sessionId={}, remaining={}", sessionId, activeSessions.size());
            });
    }

    private Map<String, Object> createNotificationFrame(CrmNotification notif) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", notif.getId() != null ? notif.getId().toString() : "ws-" + System.currentTimeMillis());
        data.put("userId", notif.getUserId());
        data.put("targetUserId", notif.getUserId());
        data.put("targetUserLogin", notif.getUserId());
        data.put("title", notif.getTitle() != null ? notif.getTitle() : "Notification");
        data.put("description", notif.getMessage() != null ? notif.getMessage() : "");
        data.put("message", notif.getMessage() != null ? notif.getMessage() : "");
        data.put("type", notif.getType() != null ? notif.getType() : "SYSTEM");
        data.put("referenceId", notif.getReferenceId());
        data.put("referenceType", notif.getReferenceType());
        data.put("isRead", Boolean.FALSE);
        data.put("timestamp", "now");
        data.put("createdAt", notif.getCreatedAt() != null ? notif.getCreatedAt().toString() : Instant.now().toString());

        if ("CHAT_MESSAGE".equalsIgnoreCase(notif.getReferenceType()) || "CHAT".equalsIgnoreCase(notif.getType())) {
            data.put("url", "/chat");
        } else if ("LEAD".equalsIgnoreCase(notif.getReferenceType()) || "NEW_LEAD".equalsIgnoreCase(notif.getType())) {
            data.put("url", "/leads");
        }

        Map<String, Object> frame = new HashMap<>();
        frame.put("event", "NOTIFICATION");
        frame.put("data", data);
        return frame;
    }
}
