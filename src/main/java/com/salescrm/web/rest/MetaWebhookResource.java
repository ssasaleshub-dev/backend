package com.salescrm.web.rest;

import com.salescrm.service.MetaGraphApiService;
import com.salescrm.service.dto.meta.MetaWebhookPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for receiving and processing official Meta (Facebook & Instagram)
 * Webhook notifications for real-time lead generation.
 */
@RestController
@RequestMapping("/api/webhooks/meta")
public class MetaWebhookResource {

    private static final Logger LOG = LoggerFactory.getLogger(MetaWebhookResource.class);

    @Value("${meta.webhook.verify-token:SSA_CRM_SECRET_2026}")
    private String verifyToken;

    private final MetaGraphApiService metaGraphApiService;

    public MetaWebhookResource(MetaGraphApiService metaGraphApiService) {
        this.metaGraphApiService = metaGraphApiService;
    }

    /**
     * Handshake verification endpoint for Meta.
     * When configuring the webhook URL in Meta App Dashboard, Meta sends a GET request
     * with hub.mode, hub.verify_token, and hub.challenge.
     */
    @GetMapping(value = "/leads", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verifyWebhook(
        @RequestParam(name = "hub.mode", required = false) String mode,
        @RequestParam(name = "hub.verify_token", required = false) String token,
        @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        LOG.info("Received Meta Webhook verification handshake request (mode={}, token={})", mode, token);

        if ("subscribe".equals(mode) && verifyToken.equals(token)) {
            LOG.info("Meta Webhook verification succeeded! Returning challenge.");
            return ResponseEntity.ok(challenge);
        }

        LOG.warn("Meta Webhook verification failed. Expected token: '{}', received: '{}'", verifyToken, token);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch");
    }

    /**
     * Real-time lead ingestion event endpoint.
     * When a user submits an Instant Form on FB or IG, Meta sends a POST request with leadgen_id.
     */
    @PostMapping(value = "/leads", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> handleLeadgenEvent(
        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
        @RequestBody MetaWebhookPayload payload
    ) {
        LOG.info("Received Meta Webhook leadgen event: object={}", payload != null ? payload.getObject() : "null");

        // Immediately return 200 OK to Meta and process lead reactively in the background
        if (payload != null && "page".equalsIgnoreCase(payload.getObject())) {
            metaGraphApiService.processWebhookPayload(payload)
                .subscribe(
                    null,
                    ex -> LOG.error("Error processing Meta webhook lead: {}", ex.getMessage(), ex)
                );
        }

        return Mono.just(ResponseEntity.ok("EVENT_RECEIVED"));
    }
}

