package com.salescrm.service;

import com.salescrm.service.dto.instagram.InstagramSendMessageRequestDTO;
import com.salescrm.service.dto.instagram.InstagramSendMessageResponseDTO;
import com.salescrm.service.dto.instagram.InstagramUserProfileDTO;
import com.salescrm.service.dto.instagram.InstagramWebhookPayloadDTO;
import java.util.Map;
import reactor.core.publisher.Mono;

/**
 * Service interface for Instagram Direct Message (DM) integration.
 * Supports incoming message webhooks, user profile resolution, CRM lead capture, and outbound messaging.
 */
public interface InstagramService {

    /**
     * Verifies the hub subscription handshake sent by Meta during Instagram webhook verification.
     *
     * @param mode hub.mode from Meta (must be "subscribe")
     * @param token hub.verify_token from Meta
     * @return true if valid, false otherwise
     */
    boolean verifySubscriptionToken(String mode, String token);

    /**
     * Asynchronously processes inbound Instagram webhook events.
     * Ingests messages, captures/updates Leads and Customers in SSA Saleshub, and triggers notifications.
     *
     * @param payload strongly typed Instagram webhook payload
     * @return Mono completing when processing completes
     */
    Mono<Void> processWebhookPayload(InstagramWebhookPayloadDTO payload);

    /**
     * Retrieves public profile information for an Instagram user (name, username, profile_pic).
     *
     * @param igsid the Instagram Scoped ID of the user
     * @return Mono of InstagramUserProfileDTO
     */
    Mono<InstagramUserProfileDTO> getUserProfile(String igsid);

    /**
     * Sends an outbound Instagram Direct Message reply to a user.
     *
     * @param request message send request containing recipientId (IGSID) and text
     * @return Mono of InstagramSendMessageResponseDTO
     */
    Mono<InstagramSendMessageResponseDTO> sendMessage(InstagramSendMessageRequestDTO request);

    /**
     * Returns the current operational status of the Instagram integration.
     *
     * @return map with configuration status, endpoints, and health
     */
    Map<String, Object> getStatus();
}
