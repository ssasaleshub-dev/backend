package com.salescrm.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.service.FacebookLeadService;
import com.salescrm.service.FacebookWebhookVerificationService;
import com.salescrm.service.dto.facebook.FacebookWebhookPayloadDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * REST controller for managing Meta/Facebook Lead Ads Webhooks.
 *
 * Implements:
 * 1. GET /api/webhooks/facebook: Meta Webhook Subscription Verification (hub.challenge handshake)
 * 2. POST /api/webhooks/facebook: Real-time leadgen event receiver with HMAC-SHA256 signature verification,
 *    sub-second HTTP 200 acknowledgment, and asynchronous reactive persistence.
 */
@RestController
@RequestMapping("/api/webhooks")
public class FacebookWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookWebhookController.class);

    private final FacebookWebhookVerificationService verificationService;
    private final FacebookLeadService facebookLeadService;
    private final ObjectMapper objectMapper;

    public FacebookWebhookController(
        FacebookWebhookVerificationService verificationService,
        FacebookLeadService facebookLeadService,
        ObjectMapper objectMapper
    ) {
        this.verificationService = verificationService;
        this.facebookLeadService = facebookLeadService;
        this.objectMapper = objectMapper;
    }

    /**
     * {@code GET /api/webhooks/facebook} : Meta webhook verification handshake.
     * When configuring the webhook URL in Meta App Dashboard, Facebook sends a GET request
     * with hub.mode, hub.verify_token, and hub.challenge.
     *
     * @param mode hub.mode query parameter (must be "subscribe")
     * @param verifyToken hub.verify_token query parameter
     * @param challenge hub.challenge query parameter
     * @return 200 OK with the challenge in plain text if valid, 403 Forbidden otherwise
     */
    @GetMapping(value = "/facebook", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<String>> verifyWebhook(
        @RequestParam(name = "hub.mode", required = false) String mode,
        @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
        @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        LOG.info("Received Meta Webhook GET verification request: mode={}, verify_token_present={}",
            mode, verifyToken != null && !verifyToken.isBlank());

        if (verificationService.verifySubscriptionToken(mode, verifyToken)) {
            LOG.info("Meta Webhook verification handshake successful. Returning challenge response.");
            return Mono.just(ResponseEntity.ok(challenge != null ? challenge : ""));
        } else {
            LOG.warn("Meta Webhook verification handshake failed: Token or mode mismatch.");
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch"));
        }
    }

    /**
     * {@code POST /api/webhooks/facebook} : Receives real-time lead notification events from Meta.
     * Validates cryptographic signature in X-Hub-Signature-256 header, immediately returns 200 OK,
     * and asynchronously retrieves full lead details from Meta Graph API for PostgreSQL persistence.
     *
     * @param signature value of X-Hub-Signature-256 header
     * @param rawPayload raw binary request body bytes for exact signature evaluation
     * @return 200 OK with "EVENT_RECEIVED" upon successful signature validation
     */
    @PostMapping(value = "/facebook", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> handleWebhookEvent(
        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
        @RequestBody byte[] rawPayload
    ) {
        LOG.info("Received Meta Webhook POST notification. Body size: {} bytes",
            rawPayload != null ? rawPayload.length : 0);

        // 1. Verify HMAC SHA-256 cryptographic signature using App Secret
        if (!verificationService.verifySignature(signature, rawPayload)) {
            LOG.error("Meta Webhook notification REJECTED: Invalid X-Hub-Signature-256 signature.");
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature"));
        }

        // 2. Parse JSON payload into strongly-typed DTO
        FacebookWebhookPayloadDTO payload;
        try {
            payload = objectMapper.readValue(rawPayload, FacebookWebhookPayloadDTO.class);
        } catch (Exception e) {
            LOG.error("Failed to parse Meta Webhook JSON payload: {}", e.getMessage(), e);
            return Mono.just(ResponseEntity.badRequest().body("Malformed JSON payload"));
        }

        // 3. Fast acknowledgment: Meta requires a 200 OK response within 20 seconds (recommended < 5s).
        // Asynchronously process the payload reactively on boundedElastic scheduler.
        if (payload != null && "page".equalsIgnoreCase(payload.getObject())) {
            facebookLeadService.processWebhookPayload(payload)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    lead -> LOG.info("Successfully ingested Meta lead: external_id={}, internal_id={}",
                        lead.getExternalId(), lead.getId()),
                    error -> LOG.error("Error asynchronously ingesting Meta lead: {}", error.getMessage(), error)
                );
        }

        return Mono.just(ResponseEntity.ok("EVENT_RECEIVED"));
    }
}
