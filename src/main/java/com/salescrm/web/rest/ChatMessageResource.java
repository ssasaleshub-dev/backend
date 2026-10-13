package com.salescrm.web.rest;

import com.salescrm.service.ChatMessageService;
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

    public ChatMessageResource(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
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
}
