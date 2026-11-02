package com.salescrm.web.rest;

import com.salescrm.service.MetaGraphApiService;
import com.salescrm.service.dto.meta.MetaWebhookPayload;
import java.util.HashMap;
import java.util.Map;
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
 * Webhook notifications for real-time lead generation and on-demand historical lead sync.
 */
@RestController
@RequestMapping({ "/api/webhooks/meta", "/api/leads/webhook" })
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
     */
    @GetMapping(value = { "/leads", "" }, produces = MediaType.TEXT_PLAIN_VALUE)
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
     */
    @PostMapping(value = { "/leads", "" }, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> handleLeadgenEvent(
        @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature,
        @RequestBody MetaWebhookPayload payload
    ) {
        LOG.info("Received Meta Webhook leadgen event: object={}", payload != null ? payload.getObject() : "null");

        if (payload != null && "page".equalsIgnoreCase(payload.getObject())) {
            metaGraphApiService.processWebhookPayload(payload)
                .subscribe(
                    null,
                    ex -> LOG.error("Error processing Meta webhook lead: {}", ex.getMessage(), ex)
                );
        }

        return Mono.just(ResponseEntity.ok("EVENT_RECEIVED"));
    }

    /**
     * Pull all existing historical leads from a Meta Form directly into the database.
     * POST /api/webhooks/meta/sync/{formId}
     */
    @PostMapping("/sync/{formId}")
    public Mono<ResponseEntity<Map<String, Object>>> syncHistoricalLeads(@PathVariable("formId") String formId) {
        LOG.info("REST request to sync all historical leads from Meta Form: {}", formId);

        return metaGraphApiService.syncHistoricalFormLeads(formId)
            .map(count -> {
                Map<String, Object> res = new HashMap<>();
                res.put("success", true);
                res.put("syncedCount", count);
                res.put("message", "Successfully pulled and saved " + count + " leads from Meta into database");
                return ResponseEntity.ok(res);
            })
            .onErrorResume(ex -> {
                Map<String, Object> err = new HashMap<>();
                err.put("success", false);
                err.put("error", ex.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err));
            });
    }
}
