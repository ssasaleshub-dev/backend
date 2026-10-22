package com.salescrm.service;

import com.salescrm.service.dto.ChatMessageDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.salescrm.domain.ChatMessage}.
 */
public interface ChatMessageService {

    /**
     * Save / post a chat message.
     *
     * @param chatMessageDTO the message to save.
     * @return the persisted and user-enriched message DTO.
     */
    Mono<ChatMessageDTO> save(ChatMessageDTO chatMessageDTO);

    /**
     * Get all chat messages ordered by creation time ascending.
     *
     * @return Flux of messages.
     */
    Flux<ChatMessageDTO> findAll();

    /**
     * Get chat messages paginated.
     *
     * @param pageable pagination info.
     * @return Flux of messages.
     */
    Flux<ChatMessageDTO> findAll(Pageable pageable);

    /**
     * Get chat messages filtered by channel ordered by creation time ascending.
     *
     * @param channel channel name (e.g. general, team).
     * @return Flux of messages.
     */
    Flux<ChatMessageDTO> findByChannel(String channel);

    /**
     * Get a chat message by id.
     *
     * @param id message id.
     * @return Mono of message DTO.
     */
    Mono<ChatMessageDTO> findOne(Long id);

    /**
     * Delete a chat message by id.
     *
     * @param id message id.
     * @return Mono signaling completion.
     */
    Mono<Void> delete(Long id);

    /**
     * Get private P2P conversation strictly between two users.
     *
     * @param userA first user id.
     * @param userB second user id.
     * @return Flux of message DTOs.
     */
    Flux<ChatMessageDTO> findP2pConversation(Long userA, Long userB);

    /**
     * Mark P2P messages sent to recipient by sender as read.
     *
     * @param recipientId recipient user id.
     * @param senderId sender user id.
     * @return Mono signaling completion.
     */
    Mono<Void> markP2pAsRead(Long recipientId, Long senderId);
}
