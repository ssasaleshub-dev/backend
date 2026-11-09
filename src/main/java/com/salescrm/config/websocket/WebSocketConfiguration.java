package com.salescrm.config.websocket;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

/**
 * Spring WebFlux WebSocket Handler Mapping configuration.
 * Exposes /api/ws/chat (proxied by Cloudflare / reverse proxy) and /ws/chat.
 */
@Configuration
public class WebSocketConfiguration {

    @Bean
    public HandlerMapping webSocketHandlerMapping(ChatWebSocketHandler chatWebSocketHandler) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/api/ws/chat", chatWebSocketHandler);
        map.put("/ws/chat", chatWebSocketHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        handlerMapping.setUrlMap(map);
        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}

