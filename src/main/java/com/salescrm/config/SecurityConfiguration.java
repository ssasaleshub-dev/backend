package com.salescrm.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import com.salescrm.security.AuthoritiesConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UserDetailsRepositoryReactiveAuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.web.server.authentication.ServerBearerTokenAuthenticationConverter;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode;
import org.springframework.security.web.server.util.matcher.NegatedServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.OrServerWebExchangeMatcher;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfiguration {

    private final JHipsterProperties jHipsterProperties;

    public SecurityConfiguration(JHipsterProperties jHipsterProperties) {
        this.jHipsterProperties = jHipsterProperties;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ReactiveAuthenticationManager reactiveAuthenticationManager(ReactiveUserDetailsService userDetailsService) {
        UserDetailsRepositoryReactiveAuthenticationManager authenticationManager = new UserDetailsRepositoryReactiveAuthenticationManager(
            userDetailsService
        );
        authenticationManager.setPasswordEncoder(passwordEncoder());
        return authenticationManager;
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .securityMatcher(
                new NegatedServerWebExchangeMatcher(
                    new OrServerWebExchangeMatcher(pathMatchers("/", "/index.html", "/favicon.ico", "/static/**", "/app/**", "/i18n/**", "/content/**", "/swagger-ui/**", "/v3/api-docs/**"))
                )
            )
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            .headers(headers ->
                headers
                    .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self' 'unsafe-inline' 'unsafe-eval' https: data: blob:;"))
                    .frameOptions(frameOptions -> frameOptions.mode(Mode.DENY))
                    .referrerPolicy(referrer ->
                        referrer.policy(ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)
                    )
            )
            .authorizeExchange(authz ->
                // prettier-ignore
                authz
                    .pathMatchers("/", "/index.html", "/favicon.ico", "/static/**").permitAll()
                    .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .pathMatchers("/ws/**").permitAll()
                    .pathMatchers("/api/ws/**").permitAll()
                    .pathMatchers("/api/authenticate").permitAll()
                    .pathMatchers("/api/register").permitAll()
                    .pathMatchers("/api/activate").permitAll()
                    .pathMatchers("/api/account/reset-password/init").permitAll()
                    .pathMatchers("/api/account/reset-password/finish").permitAll()
                    // Facebook lead webhooks are verified by FacebookWebhookVerificationService
                    .pathMatchers("/api/webhooks/**").permitAll()
                    // Google OAuth callback and auth-url
                    .pathMatchers("/api/integrations/google/callback").permitAll()
                    .pathMatchers("/api/integrations/google/auth-url").permitAll()
                    // All CRM data (leads, customers, preferences, events, notifications, chat, files,
                    // attendance) requires a logged-in user; per-lead and per-owner rules are enforced
                    // in the resources via LeadAccessService.
                    .pathMatchers("/api/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)
                    .pathMatchers("/api/**").authenticated()
                    .pathMatchers("/services/**").authenticated()
                    .pathMatchers("/management/health").permitAll()
                    .pathMatchers("/management/health/**").permitAll()
                    .pathMatchers("/management/info").permitAll()
                    .pathMatchers("/management/prometheus").permitAll()
                    .pathMatchers("/management/**").hasAuthority(AuthoritiesConstants.ADMIN)
            )
            .httpBasic(basic -> basic.disable())
            .oauth2ResourceServer(oauth2 -> oauth2.bearerTokenConverter(bearerTokenConverter()).jwt(withDefaults()));
        return http.build();
    }

    /**
     * Reads the JWT from the Authorization header. Only for the chat SSE stream (EventSource cannot
     * send headers) is the token also accepted as the {@code access_token} query parameter on GET.
     */
    private ServerAuthenticationConverter bearerTokenConverter() {
        ServerBearerTokenAuthenticationConverter headerOnly = new ServerBearerTokenAuthenticationConverter();
        ServerBearerTokenAuthenticationConverter headerOrQuery = new ServerBearerTokenAuthenticationConverter();
        headerOrQuery.setAllowUriQueryParameter(true);
        return exchange -> {
            String path = exchange.getRequest().getPath().pathWithinApplication().value();
            boolean sseStream = path.equals("/api/messages/stream") || path.equals("/api/chat/messages/stream");
            return (sseStream ? headerOrQuery : headerOnly).convert(exchange);
        };
    }
}
