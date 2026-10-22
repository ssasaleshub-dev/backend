package com.salescrm.service.impl;

import com.salescrm.domain.ChatMessage;
import com.salescrm.domain.User;
import com.salescrm.repository.ChatMessageRepository;
import com.salescrm.repository.UserRepository;
import com.salescrm.security.SecurityUtils;
import com.salescrm.service.ChatMessageService;
import com.salescrm.service.dto.ChatMessageDTO;
import com.salescrm.service.mapper.ChatMessageMapper;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link ChatMessage}.
 */
@Service
@Transactional
public class ChatMessageServiceImpl implements ChatMessageService {

    private static final Logger LOG = LoggerFactory.getLogger(ChatMessageServiceImpl.class);

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final UserRepository userRepository;

    public ChatMessageServiceImpl(
        ChatMessageRepository chatMessageRepository,
        ChatMessageMapper chatMessageMapper,
        UserRepository userRepository
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageMapper = chatMessageMapper;
        this.userRepository = userRepository;
    }

    @Override
    public Mono<ChatMessageDTO> save(ChatMessageDTO chatMessageDTO) {
        LOG.debug("Request to save ChatMessage : {}", chatMessageDTO);

        if (chatMessageDTO.getCreatedAt() == null) {
            chatMessageDTO.setCreatedAt(Instant.now());
        }
        if (chatMessageDTO.getRecipientId() != null) {
            if (chatMessageDTO.getChannel() == null || chatMessageDTO.getChannel().isBlank() || "general".equalsIgnoreCase(chatMessageDTO.getChannel())) {
                chatMessageDTO.setChannel("p2p");
            }
        } else if (chatMessageDTO.getChannel() == null || chatMessageDTO.getChannel().isBlank()) {
            chatMessageDTO.setChannel("general");
        }

        return resolveUserId(chatMessageDTO.getUserId())
            .flatMap(resolvedUserId -> {
                chatMessageDTO.setUserId(resolvedUserId);
                ChatMessage chatMessage = chatMessageMapper.toEntity(chatMessageDTO);
                return chatMessageRepository.save(chatMessage);
            })
            .map(chatMessageMapper::toDto)
            .flatMap(this::enrichDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<ChatMessageDTO> findAll() {
        LOG.debug("Request to get all ChatMessages ordered by createdAt ASC");
        return chatMessageRepository.findAllByOrderByCreatedAtAsc()
            .map(chatMessageMapper::toDto)
            .concatMap(this::enrichDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<ChatMessageDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get paginated ChatMessages: {}", pageable);
        return chatMessageRepository.findAllBy(pageable)
            .map(chatMessageMapper::toDto)
            .concatMap(this::enrichDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<ChatMessageDTO> findByChannel(String channel) {
        LOG.debug("Request to get ChatMessages by channel: {}", channel);
        String targetChannel = (channel == null || channel.isBlank()) ? "general" : channel;
        return chatMessageRepository.findAllByChannelOrderByCreatedAtAsc(targetChannel)
            .map(chatMessageMapper::toDto)
            .concatMap(this::enrichDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<ChatMessageDTO> findOne(Long id) {
        LOG.debug("Request to get ChatMessage : {}", id);
        return chatMessageRepository.findById(id)
            .map(chatMessageMapper::toDto)
            .flatMap(this::enrichDTO);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete ChatMessage : {}", id);
        return chatMessageRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<ChatMessageDTO> findP2pConversation(Long userA, Long userB) {
        LOG.debug("Request to get P2P conversation between user {} and user {}", userA, userB);
        return chatMessageRepository.findP2pConversation(userA, userB)
            .map(chatMessageMapper::toDto)
            .concatMap(this::enrichDTO);
    }

    @Override
    public Mono<Void> markP2pAsRead(Long recipientId, Long senderId) {
        LOG.debug("Request to mark P2P messages as read for recipient {} from sender {}", recipientId, senderId);
        return chatMessageRepository.markP2pAsRead(recipientId, senderId);
    }

    private Mono<Long> resolveUserId(Long providedUserId) {
        if (providedUserId != null && providedUserId > 0) {
            return Mono.just(providedUserId);
        }
        return SecurityUtils.getCurrentUserLogin()
            .flatMap(userRepository::findOneByLogin)
            .map(User::getId)
            .switchIfEmpty(
                userRepository.findAll()
                    .next()
                    .map(User::getId)
                    .defaultIfEmpty(1L)
            );
    }

    private Mono<ChatMessageDTO> enrichDTO(ChatMessageDTO dto) {
        if (dto == null) {
            return Mono.empty();
        }
        Mono<ChatMessageDTO> senderEnrich = Mono.just(dto);
        if (dto.getUserId() != null) {
            senderEnrich = userRepository.findById(dto.getUserId())
                .map(user -> {
                    dto.setUserLogin(user.getLogin());
                    String fullName = "";
                    if (user.getFirstName() != null && !user.getFirstName().isBlank()) {
                        fullName += user.getFirstName();
                    }
                    if (user.getLastName() != null && !user.getLastName().isBlank()) {
                        fullName += (fullName.isBlank() ? "" : " ") + user.getLastName();
                    }
                    if (fullName.isBlank()) {
                        fullName = user.getLogin();
                    }
                    dto.setUserName(fullName);
                    dto.setUserImageUrl(user.getImageUrl());
                    return dto;
                })
                .defaultIfEmpty(dto);
        }

        return senderEnrich.flatMap(d -> {
            if (d.getRecipientId() != null) {
                return userRepository.findById(d.getRecipientId())
                    .map(recipient -> {
                        d.setRecipientLogin(recipient.getLogin());
                        String fullName = "";
                        if (recipient.getFirstName() != null && !recipient.getFirstName().isBlank()) {
                            fullName += recipient.getFirstName();
                        }
                        if (recipient.getLastName() != null && !recipient.getLastName().isBlank()) {
                            fullName += (fullName.isBlank() ? "" : " ") + recipient.getLastName();
                        }
                        if (fullName.isBlank()) {
                            fullName = recipient.getLogin();
                        }
                        d.setRecipientName(fullName);
                        return d;
                    })
                    .defaultIfEmpty(d);
            }
            return Mono.just(d);
        });
    }
}
