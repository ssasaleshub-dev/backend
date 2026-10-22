package com.salescrm.repository;

import com.salescrm.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the ChatMessage entity.
 */
@SuppressWarnings("unused")
@Repository
public interface ChatMessageRepository extends ReactiveCrudRepository<ChatMessage, Long>, ChatMessageRepositoryInternal {

    Flux<ChatMessage> findAllByOrderByCreatedAtAsc();

    Flux<ChatMessage> findAllByOrderByCreatedAtDesc();

    Flux<ChatMessage> findAllByChannelOrderByCreatedAtAsc(String channel);

    Flux<ChatMessage> findAllByChannelOrderByCreatedAtDesc(String channel);

    Flux<ChatMessage> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT * FROM messages WHERE (user_id = :userA AND recipient_id = :userB) OR (user_id = :userB AND recipient_id = :userA) ORDER BY created_at ASC")
    Flux<ChatMessage> findP2pConversation(Long userA, Long userB);

    @Modifying
    @Query("UPDATE messages SET is_read = true WHERE recipient_id = :recipientId AND user_id = :senderId AND is_read = false")
    Mono<Void> markP2pAsRead(Long recipientId, Long senderId);

    @Override
    <S extends ChatMessage> Mono<S> save(S entity);

    @Override
    Flux<ChatMessage> findAll();

    @Override
    Mono<ChatMessage> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);
}

interface ChatMessageRepositoryInternal {
    <S extends ChatMessage> Mono<S> save(S entity);

    Flux<ChatMessage> findAllBy(Pageable pageable);

    Flux<ChatMessage> findAll();

    Mono<ChatMessage> findById(Long id);
}
