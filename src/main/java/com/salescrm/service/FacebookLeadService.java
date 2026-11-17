package com.salescrm.service;

import com.salescrm.domain.Lead;
import com.salescrm.service.dto.facebook.FacebookWebhookPayloadDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for orchestrating Facebook Lead Ads integration.
 * Handles webhook notifications, Meta Graph API retrieval, idempotency guards,
 * and reactive persistence to PostgreSQL.
 */
public interface FacebookLeadService {

    /**
     * Reactively processes all entries and changes in a Meta Webhook payload.
     *
     * @param payload the parsed webhook payload from Meta
     * @return Flux of successfully processed or verified Leads
     */
    Flux<Lead> processWebhookPayload(FacebookWebhookPayloadDTO payload);

    /**
     * Processes a single Facebook lead by leadgen_id and page_id.
     * Guarantees idempotency to prevent duplicate insertion.
     *
     * @param leadgenId the Meta leadgen ID
     * @param pageId the Meta Facebook Page ID
     * @param adId optional Ad ID from webhook event
     * @param formId optional Form ID from webhook event
     * @return Mono of the persisted Lead
     */
    Mono<Lead> processLead(String leadgenId, String pageId, String adId, String formId);
}
