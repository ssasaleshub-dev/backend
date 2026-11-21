package com.salescrm.config.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.domain.CrmNotification;
import com.salescrm.service.ChatMessageService;
import com.salescrm.service.CrmNotificationService;
import com.salescrm.service.dto.ChatMessageDTO;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring WebFlux Reactive WebSocket Handler for Team Chat & P2P Direct Messaging.
 * Operates on /api/ws/chat and /ws/chat, allowing bidirectional real-time communication
 * with instant message broadcasting and live notification delivery.
 */
@Component
public class ChatWebSocketHandler implements WebSocketHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final ChatMessageService chatMessageService;
    private final CrmNotificationService crmNotificationService;
    private final ObjectMapper objectMapper;
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(
        ChatMessageService chatMessageService,
        @Autowired(required = false) CrmNotificationService crmNotificationService,
        ObjectMapper objectMapper
    ) {
        this.chatMessageService = chatMessageService;
        this.crmNotificationService = crmNotificationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        LOG.info("[ChatWebSocket] 🟢 Client connected: sessionId={}, total active={}", sessionId, activeSessions.size());

        // Send CONNECTED handshake frame
        Mono<Void> initialHandshake = session.send(
            Mono.fromCallable(() -> {
                Map<String, Object> welcome = Map.of(
                    "event", "CONNECTED",
                    "sessionId", sessionId,
                    "service", "Spring WebFlux Realtime Chat"
                );
                return session.textMessage(objectMapper.writeValueAsString(welcome));
            })
        );

        // Stream outgoing live messages from the shared reactive sink to this client
        Flux<WebSocketMessage> outgoingChat = chatMessageService.getLiveMessageStream()
            .flatMap(dto -> {
                try {
                    Map<String, Object> frame = Map.of(
                        "event", "NEW_MESSAGE",
                        "message", dto
                    );
                    String json = objectMapper.writeValueAsString(frame);
                    return Mono.just(session.textMessage(json));
                } catch (Exception e) {
                    LOG.error("[ChatWebSocket] Error serializing message frame: {}", e.getMessage());
                    return Mono.empty();
                }
            });

        // Also stream live notification frames over chat WebSocket for zero-lag alerts
        Flux<WebSocketMessage> outgoingNotifications = (crmNotificationService != null)
            ? crmNotificationService.getLiveNotificationStream()
                .flatMap(notif -> {
                    try {
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

                        Map<String, Object> frame = Map.of(
                            "event", "NOTIFICATION",
                            "data", data
                        );
                        String json = objectMapper.writeValueAsString(frame);
                        return Mono.just(session.textMessage(json));
                    } catch (Exception e) {
                        LOG.error("[ChatWebSocket] Error serializing notification frame: {}", e.getMessage());
                        return Mono.empty();
                    }
                })
            : Flux.empty();

        Flux<WebSocketMessage> outgoingFlux = Flux.merge(outgoingChat, outgoingNotifications);

        // Handle incoming client messages
        Mono<Void> incomingHandler = session.receive()
            .map(WebSocketMessage::getPayloadAsText)
            .flatMap(payload -> {
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> frame = objectMapper.readValue(payload, Map.class);
                    String event = (String) frame.get("event");
                    LOG.debug("[ChatWebSocket] Received frame: {}", event);

                    if ("SEND_MESSAGE".equalsIgnoreCase(event)) {
                        Object payloadObj = frame.get("payload");
                        if (payloadObj != null) {
                            ChatMessageDTO dto = objectMapper.convertValue(payloadObj, ChatMessageDTO.class);
                            return chatMessageService.save(dto).then();
                        }
                    } else if ("AUTHENTICATE".equalsIgnoreCase(event)) {
                        LOG.debug("[ChatWebSocket] Session {} authenticated", sessionId);
                    }
                } catch (Exception e) {
                    LOG.warn("[ChatWebSocket] Failed to parse message frame: {}", e.getMessage());
                }
                return Mono.empty();
            })
            .then();

        return initialHandshake
            .then(session.send(outgoingFlux).and(incomingHandler))
            .doFinally(signalType -> {
                activeSessions.remove(sessionId);
                LOG.info("[ChatWebSocket] 🔴 Client disconnected: sessionId={}, remaining={}", sessionId, activeSessions.size());
            });
    }
}

