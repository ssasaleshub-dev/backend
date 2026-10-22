package com.salescrm.web.rest;

import com.salescrm.security.SecurityUtils;
import com.salescrm.service.ChatMessageService;
import com.salescrm.service.UserService;
import com.salescrm.service.dto.ChatMessageDTO;
import com.salescrm.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing team chat messages (table messages).
 */
@RestController
@RequestMapping({ "/api/messages", "/api/chat/messages" })
public class ChatMessageResource {

    private static final Logger LOG = LoggerFactory.getLogger(ChatMessageResource.class);

    private static final String ENTITY_NAME = "chatMessage";

    @Value("${jhipster.clientApp.name:salesCRM}")
    private String applicationName;

    private final ChatMessageService chatMessageService;
    private final UserService userService;

    public ChatMessageResource(ChatMessageService chatMessageService, UserService userService) {
        this.chatMessageService = chatMessageService;
        this.userService = userService;
    }

    /**
     * {@code POST  /api/messages} : Post a new chat message.
     *
     * @param chatMessageDTO the chat message to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new chatMessageDTO.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<ChatMessageDTO>> createMessage(@Valid @RequestBody ChatMessageDTO chatMessageDTO) throws URISyntaxException {
        LOG.debug("REST request to save team chat message : {}", chatMessageDTO);
        if (chatMessageDTO.getId() != null) {
            throw new BadRequestAlertException("A new chat message cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return chatMessageService
            .save(chatMessageDTO)
            .map(result -> {
                try {
                    return ResponseEntity.created(new URI("/api/messages/" + result.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                        .body(result);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    /**
     * {@code GET  /api/messages} : Get all chat messages.
     * Supports optional channel filtering and pagination.
     *
     * @param channel optional channel name (e.g. general, team).
     * @param page optional page index (0-based).
     * @param size optional page size.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of messages in body.
     */
    @GetMapping("")
    public Mono<ResponseEntity<List<ChatMessageDTO>>> getAllMessages(
        @RequestParam(name = "channel", required = false) String channel,
        @RequestParam(name = "page", required = false) Integer page,
        @RequestParam(name = "size", required = false) Integer size
    ) {
        LOG.debug("REST request to get chat messages. channel: {}, page: {}, size: {}", channel, page, size);

        if (channel != null && !channel.isBlank()) {
            return chatMessageService.findByChannel(channel)
                .collectList()
                .map(ResponseEntity::ok);
        }

        if (page != null && size != null && size > 0) {
            return chatMessageService.findAll(PageRequest.of(page, size))
                .collectList()
                .map(ResponseEntity::ok);
        }

        return chatMessageService.findAll()
            .collectList()
            .map(ResponseEntity::ok);
    }

    /**
     * {@code GET  /api/messages/channel/:channel} : Get all messages for a specific channel.
     *
     * @param channel channel name.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of messages in body.
     */
    @GetMapping("/channel/{channel}")
    public Mono<ResponseEntity<List<ChatMessageDTO>>> getMessagesByChannel(@PathVariable String channel) {
        LOG.debug("REST request to get chat messages for channel: {}", channel);
        return chatMessageService.findByChannel(channel)
            .collectList()
            .map(ResponseEntity::ok);
    }

    /**
     * {@code GET  /api/messages/:id} : Get the chat message with the given ID.
     *
     * @param id the id of the chatMessageDTO to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the chatMessageDTO, or with status {@code 404 (Not Found)}.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<ChatMessageDTO>> getMessage(@PathVariable Long id) {
        LOG.debug("REST request to get ChatMessage : {}", id);
        return ResponseUtil.wrapOrNotFound(chatMessageService.findOne(id));
    }

    /**
     * {@code DELETE  /api/messages/:id} : Delete the chat message with the given ID.
     *
     * @param id the id of the chatMessageDTO to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteMessage(@PathVariable Long id) {
        LOG.debug("REST request to delete ChatMessage : {}", id);
        return chatMessageService
            .delete(id)
            .thenReturn(
                ResponseEntity.noContent()
                    .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                    .build()
            );
    }

    /**
     * {@code GET  /api/messages/p2p/:otherUserId} : Get private P2P conversation strictly between the current user and otherUserId.
     *
     * @param otherUserId the other user ID.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of chat messages.
     */
    @GetMapping("/p2p/{otherUserId}")
    public Mono<ResponseEntity<List<ChatMessageDTO>>> getP2pMessages(@PathVariable Long otherUserId) {
        LOG.debug("REST request to get P2P messages with user: {}", otherUserId);
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userService::getUserWithAuthoritiesByLogin)
            .flatMap(currentUser ->
                chatMessageService
                    .findP2pConversation(currentUser.getId(), otherUserId)
                    .collectList()
                    .flatMap(messages ->
                        chatMessageService.markP2pAsRead(currentUser.getId(), otherUserId)
                            .thenReturn(messages)
                    )
            )
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.ok(List.of()));
    }

    /**
     * {@code PATCH  /api/messages/p2p/:otherUserId/read} : Mark all unread messages from otherUserId to current user as read.
     *
     * @param otherUserId the sender user ID.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @PatchMapping("/p2p/{otherUserId}/read")
    public Mono<ResponseEntity<Void>> markP2pAsRead(@PathVariable Long otherUserId) {
        LOG.debug("REST request to mark P2P messages from user {} as read", otherUserId);
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userService::getUserWithAuthoritiesByLogin)
            .flatMap(currentUser -> chatMessageService.markP2pAsRead(currentUser.getId(), otherUserId))
            .thenReturn(ResponseEntity.noContent().build());
    }
}
