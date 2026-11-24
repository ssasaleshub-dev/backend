package com.salescrm.web.rest;

import com.salescrm.service.InstagramService;
import com.salescrm.service.dto.instagram.InstagramSendMessageRequestDTO;
import com.salescrm.service.dto.instagram.InstagramSendMessageResponseDTO;
import com.salescrm.service.dto.instagram.InstagramUserProfileDTO;
import jakarta.validation.Valid;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing Instagram DM integration within SSA Saleshub CRM.
 * Provides endpoints for sending direct messages, querying profiles, and checking health/status.
 */
@RestController
@RequestMapping("/api/integrations/instagram")
public class InstagramResource {

    private static final Logger LOG = LoggerFactory.getLogger(InstagramResource.class);

    private final InstagramService instagramService;

    public InstagramResource(InstagramService instagramService) {
        this.instagramService = instagramService;
    }

    /**
     * {@code GET /api/integrations/instagram/status} : Checks integration configuration status.
     *
     * @return current status map
     */
    @GetMapping("/status")
    public Mono<ResponseEntity<Map<String, Object>>> getStatus() {
        return Mono.just(ResponseEntity.ok(instagramService.getStatus()));
    }

    /**
     * {@code POST /api/integrations/instagram/send-message} : Sends an outbound Instagram DM reply.
     *
     * @param request the send message request payload
     * @return result containing recipientId and messageId
     */
    @PostMapping("/send-message")
    public Mono<ResponseEntity<InstagramSendMessageResponseDTO>> sendMessage(
        @Valid @RequestBody InstagramSendMessageRequestDTO request
    ) {
        LOG.info("Received request to send Instagram DM reply to recipient={}", request.getRecipientId());
        return instagramService.sendMessage(request).map(ResponseEntity::ok);
    }

    /**
     * {@code GET /api/integrations/instagram/profile/{igsid}} : Resolves user public profile info.
     *
     * @param igsid the Instagram scoped id
     * @return user profile DTO
     */
    @GetMapping("/profile/{igsid}")
    public Mono<ResponseEntity<InstagramUserProfileDTO>> getProfile(@PathVariable String igsid) {
        return instagramService.getUserProfile(igsid).map(ResponseEntity::ok);
    }
}
