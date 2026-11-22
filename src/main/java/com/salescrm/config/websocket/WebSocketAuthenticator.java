package com.salescrm.config.websocket;

import com.salescrm.security.SecurityUtils;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * Authenticates WebSocket connections. Browsers cannot set an Authorization header on a WebSocket
 * handshake, so the client passes its JWT as the {@code access_token} query parameter. The token is
 * verified with the same decoder as REST requests; connections without a valid token are refused.
 */
@Component
public class WebSocketAuthenticator {

    private static final Logger LOG = LoggerFactory.getLogger(WebSocketAuthenticator.class);

    private final ReactiveJwtDecoder jwtDecoder;

    public WebSocketAuthenticator(ReactiveJwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    /** The authenticated user behind a socket. */
    public record SocketUser(String login, String numericId, Set<String> authorities) {
        /** True when a notification/message addressed to {@code target} belongs to this user. */
        public boolean isTarget(String target) {
            if (target == null || target.isBlank()) {
                return false;
            }
            String t = target.trim();
            return t.equalsIgnoreCase(login) || (numericId != null && !numericId.isEmpty() && t.equals(numericId));
        }
    }

    /** Emits the socket's user, or completes empty when the token is missing or invalid. */
    public Mono<SocketUser> authenticate(WebSocketSession session) {
        String token = UriComponentsBuilder.fromUri(session.getHandshakeInfo().getUri())
            .build()
            .getQueryParams()
            .getFirst("access_token");
        if (token == null || token.isBlank()) {
            LOG.debug("[WebSocket] Rejecting connection without access_token");
            return Mono.empty();
        }
        return jwtDecoder
            .decode(token)
            .map(this::toUser)
            .onErrorResume(e -> {
                LOG.debug("[WebSocket] Rejecting connection with invalid token: {}", e.getMessage());
                return Mono.empty();
            });
    }

    private SocketUser toUser(Jwt jwt) {
        Object userId = jwt.getClaims().get(SecurityUtils.USER_ID_CLAIM);
        Object rawAuthorities = jwt.getClaims().get(SecurityUtils.AUTHORITIES_CLAIM);
        Set<String> authorities;
        if (rawAuthorities instanceof String s) {
            authorities = Set.of(s.split("[\\s,]+"));
        } else if (rawAuthorities instanceof List<?> list) {
            authorities = list.stream().map(String::valueOf).collect(Collectors.toSet());
        } else {
            authorities = Collections.emptySet();
        }
        String login = jwt.getSubject() != null ? jwt.getSubject().toLowerCase() : "";
        return new SocketUser(login, userId != null ? String.valueOf(userId) : "", authorities);
    }
}
