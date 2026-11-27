package com.salescrm.service.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.config.FacebookProperties;
import com.salescrm.service.dto.facebook.FacebookLeadGraphResponseDTO;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Reactive client for communicating with Meta Graph API.
 * Uses non-blocking Spring WebClient with exponential backoff retries and structured error handling.
 */
@Component
public class FacebookGraphApiClient {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookGraphApiClient.class);

    private final WebClient webClient;
    private final FacebookProperties facebookProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Forms whose name lookup failed recently: don't ask Meta again for every new lead. */
    private final java.util.concurrent.ConcurrentHashMap<String, java.time.Instant> formNameFailures = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Duration FORM_NAME_RETRY_AFTER = Duration.ofHours(6);

    public FacebookGraphApiClient(FacebookProperties facebookProperties, WebClient.Builder webClientBuilder) {
        this.facebookProperties = facebookProperties;
        this.webClient = webClientBuilder
            .baseUrl(facebookProperties.getGraphApiBaseUrl())
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    /**
     * Retrieves full lead details from Meta Graph API using the configured Page Access Token.
     *
     * @param leadgenId the Meta leadgen ID
     * @return Mono of FacebookLeadGraphResponseDTO
     */
    public Mono<FacebookLeadGraphResponseDTO> getLeadDetails(String leadgenId) {
        return getLeadDetails(leadgenId, facebookProperties.getPageAccessToken());
    }

    /**
     * Retrieves full lead details from Meta Graph API using a specific access token.
     *
     * @param leadgenId the Meta leadgen ID
     * @param accessToken Page Access Token
     * @return Mono of FacebookLeadGraphResponseDTO
     */
    public Mono<FacebookLeadGraphResponseDTO> getLeadDetails(String leadgenId, String accessToken) {
        if (leadgenId == null || leadgenId.isBlank()) {
            return Mono.error(new IllegalArgumentException("leadgenId must not be null or blank"));
        }

        if (accessToken == null || accessToken.isBlank()) {
            LOG.error("Cannot fetch Meta lead details: Page Access Token is not configured");
            return Mono.error(new IllegalStateException("Meta Page Access Token is missing"));
        }

        String path = String.format("/%s/%s", facebookProperties.getGraphApiVersion(), leadgenId);

        LOG.info("Calling Meta Graph API for leadgen_id: {}", leadgenId);

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(path)
                .queryParam("fields", "id,created_time,ad_id,ad_name,adset_id,adset_name,campaign_id,campaign_name,form_id,is_organic,platform,field_data")
                .queryParam("access_token", accessToken)
                .build()
            )
            .retrieve()
            .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                clientResponse.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        LOG.error("Meta Graph API 4xx error for lead {}: HTTP {} - {}",
                            leadgenId, clientResponse.statusCode(), sanitizeError(errorBody));
                        return Mono.error(new WebClientResponseException(
                            clientResponse.statusCode().value(),
                            "Meta Graph API Client Error: " + sanitizeError(errorBody),
                            clientResponse.headers().asHttpHeaders(),
                            errorBody.getBytes(),
                            null
                        ));
                    })
            )
            .onStatus(HttpStatusCode::is5xxServerError, serverResponse ->
                serverResponse.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        LOG.error("Meta Graph API 5xx error for lead {}: HTTP {} - {}",
                            leadgenId, serverResponse.statusCode(), sanitizeError(errorBody));
                        return Mono.error(new WebClientResponseException(
                            serverResponse.statusCode().value(),
                            "Meta Graph API Server Error",
                            serverResponse.headers().asHttpHeaders(),
                            errorBody.getBytes(),
                            null
                        ));
                    })
            )
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(20))
            .map(this::parseLeadResponse)
            .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                .filter(this::isTransientError)
                .doBeforeRetry(retrySignal ->
                    LOG.warn("Retrying Meta Graph API call for lead {} (attempt {}) due to: {}",
                        leadgenId, retrySignal.totalRetries() + 1, retrySignal.failure().getMessage())
                )
            )
            .doOnSuccess(resp -> LOG.info("Successfully retrieved lead details from Meta for lead ID: {}", leadgenId))
            .doOnError(err -> LOG.error("Failed to retrieve lead details from Meta for lead {}: {}", leadgenId, err.getMessage()));
    }

    /** Parses the Graph response and keeps the raw JSON so nothing Meta sent is lost. */
    private FacebookLeadGraphResponseDTO parseLeadResponse(String body) {
        try {
            FacebookLeadGraphResponseDTO dto = objectMapper.readValue(body, FacebookLeadGraphResponseDTO.class);
            dto.setRawJson(body);
            return dto;
        } catch (Exception e) {
            // Not retryable: the same body would fail again
            throw new IllegalStateException("Unreadable Meta Graph API lead response", e);
        }
    }

    /**
     * Looks up an Instant Form's real name (GET /{version}/{formId}?fields=name). Best effort:
     * completes empty on any error so lead ingestion never fails because of it.
     */
    public Mono<String> getFormName(String formId) {
        return getFormName(formId, facebookProperties.getPageAccessToken());
    }

    /**
     * Looks up an Instant Form's real name using a specific Page Access Token.
     */
    public Mono<String> getFormName(String formId, String accessToken) {
        if (formId == null || formId.isBlank() || accessToken == null || accessToken.isBlank()) {
            return Mono.empty();
        }
        java.time.Instant lastFailure = formNameFailures.get(formId);
        if (lastFailure != null && lastFailure.plus(FORM_NAME_RETRY_AFTER).isAfter(java.time.Instant.now())) {
            return Mono.empty();
        }
        String path = String.format("/%s/%s", facebookProperties.getGraphApiVersion(), formId);
        return webClient.get()
            .uri(uriBuilder -> uriBuilder.path(path).queryParam("fields", "name").queryParam("access_token", accessToken).build())
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(10))
            .flatMap(body -> {
                try {
                    String name = objectMapper.readTree(body).path("name").asText(null);
                    return name != null && !name.isBlank() ? Mono.just(name.trim()) : Mono.<String>empty();
                } catch (Exception e) {
                    return Mono.<String>empty();
                }
            })
            .doOnNext(name -> formNameFailures.remove(formId))
            .onErrorResume(err -> {
                formNameFailures.put(formId, java.time.Instant.now());
                LOG.warn("Could not look up Meta form name for form {} (will retry after {}): {}", formId, FORM_NAME_RETRY_AFTER, sanitizeError(err.getMessage()));
                return Mono.empty();
            });
    }

    private boolean isTransientError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException webEx) {
            // Retry on 5xx or rate limiting (HTTP 429)
            return webEx.getStatusCode().is5xxServerError() || webEx.getStatusCode().value() == 429;
        }
        return !(throwable instanceof IllegalArgumentException || throwable instanceof IllegalStateException);
    }

    private String sanitizeError(String rawError) {
        if (rawError == null) return "";
        // Redact access tokens in log outputs
        return rawError.replaceAll("access_token=[^&\"']+", "access_token=REDACTED");
    }
}
