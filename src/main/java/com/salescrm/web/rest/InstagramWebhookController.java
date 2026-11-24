package com.salescrm.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.service.FacebookWebhookVerificationService;
import com.salescrm.service.InstagramService;
import com.salescrm.service.dto.instagram.InstagramWebhookPayloadDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * REST controller for managing Meta/Instagram Direct Message Webhooks.
 *
 * Implements:
 * 1. GET /api/webhooks/instagram: Meta Webhook Subscription Verification (hub.challenge handshake)
 * 2. POST /api/webhooks/instagram: Real-time Instagram DM event receiver with HMAC-SHA256 signature verification,
 *    instant HTTP 200 acknowledgment, and non-blocking reactive lead ingestion.
 */
@RestController
@RequestMapping("/api/webhooks")
public class InstagramWebhookController {

    private static final Logger LOG = LoggerFactory.getLogger(InstagramWebhookController.class);

    private final InstagramService instagramService;
    private final FacebookWebhookVerificationService verificationService;
    private final ObjectMapper objectMapper;

    public InstagramWebhookController(
        InstagramService instagramService,
        FacebookWebhookVerificationService verificationService,
        ObjectMapper objectMapper
    ) {
        this.instagramService = instagramService;
        this.verificationService = verificationService;
        this.objectMapper = objectMapper;
    }

    /**
     * {@code GET /api/webhooks/instagram} : Meta webhook verification handshake.
     * When configuring the webhook in Meta Developer Dashboard, Meta sends a GET request
     * with hub.mode, hub.verify_token, and hub.challenge.
     *
     * @param mode hub.mode query parameter (must be "subscribe")
     * @param verifyToken hub.verify_token query parameter
     * @param challenge hub.challenge query parameter
     * @return 200 OK with the challenge in plain text if valid, 403 Forbidden otherwise
     */
    @GetMapping(value = "/instagram", produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<String>> verifyWebhook(
        @RequestParam(name = "hub.mode", required = false) String mode,
        @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
        @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        LOG.info("Received Meta Instagram Webhook GET verification request: mode={}, verify_token_present={}",
            mode, verifyToken != null && !verifyToken.isBlank());

        if (instagramService.verifySubscriptionToken(mode, verifyToken) ||
            verificationService.verifySubscriptionToken(mode, verifyToken)) {
            LOG.info("Instagram Webhook verification handshake successful. Returning challenge response.");
            return Mono.just(ResponseEntity.ok(challenge != null ? challenge : ""));
        } else {
            LOG.warn("Instagram Webhook verification handshake failed: Token or mode mismatch.");
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body("Verification token mismatch"));
        }
    }

    /**
     * {@code POST /api/webhooks/instagram} : Receives real-time messaging events from Instagram.
     * Validates cryptographic signature in X-Hub-Signature-256 header, immediately returns 200 OK,
     * and asynchronously processes the messages.
     *
     * @param signature value of X-Hub-Signature-256 header
     * @param rawPayload raw binary request body bytes
     * @return 200 OK with "EVENT_RECEIVED" upon successful signature validation
     */
    @PostMapping(value = "/instagram", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> handleWebhookEvent(
        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
        @RequestBody byte[] rawPayload
    ) {
        LOG.info("Received Instagram Webhook POST notification. Body size: {} bytes",
            rawPayload != null ? rawPayload.length : 0);

        // 1. Verify HMAC SHA-256 cryptographic signature using App Secret
        if (!verificationService.verifySignature(signature, rawPayload)) {
            LOG.error("Instagram Webhook notification REJECTED: Invalid X-Hub-Signature-256 signature.");
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid signature"));
        }

        // 2. Parse JSON payload into strongly-typed DTO
        InstagramWebhookPayloadDTO payload;
        try {
            payload = objectMapper.readValue(rawPayload, InstagramWebhookPayloadDTO.class);
        } catch (Exception e) {
            LOG.error("Failed to parse Instagram Webhook JSON payload: {}", e.getMessage(), e);
            // Return 200 to acknowledge delivery and prevent Meta from disabling the webhook
            return Mono.just(ResponseEntity.ok("EVENT_RECEIVED"));
        }

        // 3. Asynchronously process payload on boundedElastic scheduler
        instagramService.processWebhookPayload(payload)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnError(err -> LOG.error("Error during asynchronous Instagram DM processing: {}", err.getMessage(), err))
            .subscribe();

        // 4. Immediately return HTTP 200 OK to fulfill Meta's <5s response requirement
        return Mono.just(ResponseEntity.ok("EVENT_RECEIVED"));
    }
}
