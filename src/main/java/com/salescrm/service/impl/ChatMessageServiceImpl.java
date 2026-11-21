package com.salescrm.service.impl;

import com.salescrm.domain.ChatMessage;
import com.salescrm.domain.CrmNotification;
import com.salescrm.domain.User;
import com.salescrm.repository.ChatMessageRepository;
import com.salescrm.repository.UserRepository;
import com.salescrm.security.SecurityUtils;
import com.salescrm.service.ChatMessageService;
import com.salescrm.service.CrmNotificationService;
import com.salescrm.service.FirebasePushNotificationService;
import com.salescrm.service.dto.ChatMessageDTO;
import com.salescrm.service.mapper.ChatMessageMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

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
    private final FirebasePushNotificationService pushNotificationService;
    private final CrmNotificationService crmNotificationService;
    private final Sinks.Many<ChatMessageDTO> chatSink = Sinks.many().multicast().onBackpressureBuffer();

    public ChatMessageServiceImpl(
        ChatMessageRepository chatMessageRepository,
        ChatMessageMapper chatMessageMapper,
        UserRepository userRepository,
        @Autowired(required = false) FirebasePushNotificationService pushNotificationService
    ) {
        this(chatMessageRepository, chatMessageMapper, userRepository, pushNotificationService, null);
    }

    @Autowired
    public ChatMessageServiceImpl(
        ChatMessageRepository chatMessageRepository,
        ChatMessageMapper chatMessageMapper,
        UserRepository userRepository,
        @Autowired(required = false) FirebasePushNotificationService pushNotificationService,
        @Autowired(required = false) CrmNotificationService crmNotificationService
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.chatMessageMapper = chatMessageMapper;
        this.userRepository = userRepository;
        this.pushNotificationService = pushNotificationService;
        this.crmNotificationService = crmNotificationService;
    }

    @Override
    public Mono<ChatMessageDTO> save(ChatMessageDTO chatMessageDTO) {
        LOG.debug("Request to save ChatMessage : {}", chatMessageDTO);

        if (chatMessageDTO.getCreatedAt() == null) {
            chatMessageDTO.setCreatedAt(Instant.now());
        }

        return resolveRecipient(chatMessageDTO)
            .flatMap(dto -> resolveUserId(dto.getUserId())
                .flatMap(resolvedUserId -> {
                    dto.setUserId(resolvedUserId);
                    ChatMessage chatMessage = chatMessageMapper.toEntity(dto);
                    return chatMessageRepository.save(chatMessage);
                })
            )
            .map(chatMessageMapper::toDto)
            .flatMap(this::enrichDTO)
            .doOnNext(savedDto -> {
                broadcastMessage(savedDto);
                sendChatPushNotification(savedDto);
            });
    }

    private Mono<ChatMessageDTO> resolveRecipient(ChatMessageDTO dto) {
        if (dto.getRecipientId() != null) {
            if (dto.getChannel() == null || dto.getChannel().isBlank() || "general".equalsIgnoreCase(dto.getChannel())) {
                dto.setChannel("p2p");
            }
            return Mono.just(dto);
        }

        String potentialLogin = null;
        if (dto.getRecipientLogin() != null && !dto.getRecipientLogin().isBlank()) {
            potentialLogin = dto.getRecipientLogin().trim().toLowerCase();
        } else if (dto.getChannel() != null && !dto.getChannel().isBlank()
            && !"general".equalsIgnoreCase(dto.getChannel()) && !"p2p".equalsIgnoreCase(dto.getChannel())) {
            potentialLogin = dto.getChannel().trim().toLowerCase();
        }

        if (potentialLogin != null) {
            return userRepository.findOneByLogin(potentialLogin)
                .map(user -> {
                    dto.setRecipientId(user.getId());
                    dto.setRecipientLogin(user.getLogin());
                    dto.setChannel("p2p");
                    return dto;
                })
                .defaultIfEmpty(dto);
        }

        if (dto.getChannel() == null || dto.getChannel().isBlank()) {
            dto.setChannel("general");
        }
        return Mono.just(dto);
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

    @Override
    public Flux<ChatMessageDTO> getLiveMessageStream() {
        return chatSink.asFlux();
    }

    @Override
    public void broadcastMessage(ChatMessageDTO message) {
        if (message != null) {
            LOG.info("[LiveChat] Broadcasting message id={} channel={} from={}", message.getId(), message.getChannel(), message.getUserName());
            chatSink.tryEmitNext(message);
        }
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

    private void sendChatPushNotification(ChatMessageDTO dto) {
        if (dto == null) return;

        try {
            String senderName = dto.getUserName() != null && !dto.getUserName().isBlank()
                ? dto.getUserName()
                : (dto.getUserLogin() != null ? dto.getUserLogin() : "Colleague");

            String contentPreview = dto.getContent();
            if (contentPreview == null || contentPreview.isBlank()) {
                if ("IMAGE".equalsIgnoreCase(dto.getMediaType()) || (dto.getMediaUrl() != null && dto.getMediaUrl().matches(".*\\.(jpg|jpeg|png|webp|gif).*"))) {
                    contentPreview = "Photo";
                } else if ("VIDEO".equalsIgnoreCase(dto.getMediaType())) {
                    contentPreview = "Video";
                } else if (dto.getFileName() != null) {
                    contentPreview = "File: " + dto.getFileName();
                } else if (dto.getMediaUrl() != null) {
                    contentPreview = "Attachment";
                } else {
                    contentPreview = "Sent a message";
                }
            }

            Map<String, String> data = new HashMap<>();
            data.put("type", "CHAT");
            data.put("route", "/chat");
            data.put("click_action", "OPEN_CHAT");
            data.put("messageId", dto.getId() != null ? dto.getId().toString() : "");
            data.put("senderId", dto.getUserId() != null ? dto.getUserId().toString() : "");
            data.put("senderName", senderName);

            if (dto.getRecipientId() != null || (dto.getRecipientLogin() != null && !dto.getRecipientLogin().isBlank())) {
                // Direct P2P Chat Push Notification
                String recipientIdStr = dto.getRecipientId() != null ? dto.getRecipientId().toString() : "";
                String recipientLoginStr = dto.getRecipientLogin();

                data.put("chatType", "p2p");
                data.put("channel", "p2p");
                if (!recipientIdStr.isBlank()) {
                    data.put("recipientId", recipientIdStr);
                }
                if (recipientLoginStr != null) {
                    data.put("recipientLogin", recipientLoginStr);
                }

                String title = senderName;
                String body = contentPreview;
                if (pushNotificationService != null) {
                    pushNotificationService.sendChatPushNotification(
                        recipientIdStr,
                        recipientLoginStr,
                        title,
                        body,
                        data
                    );
                }

                // MNC Standard: Persist in-app notification in crm_notifications table for recipient
                if (crmNotificationService != null) {
                    String targetUserId = (recipientLoginStr != null && !recipientLoginStr.isBlank())
                        ? recipientLoginStr.trim().toLowerCase()
                        : recipientIdStr;
                    CrmNotification notif = new CrmNotification();
                    notif.setUserId(targetUserId);
                    notif.setTitle(title);
                    notif.setMessage(body);
                    notif.setType("CHAT");
                    notif.setReferenceId(dto.getId() != null ? dto.getId().toString() : null);
                    notif.setReferenceType("CHAT_MESSAGE");
                    notif.setIsRead(Boolean.FALSE);
                    notif.setCreatedAt(Instant.now());
                    crmNotificationService.createNotification(notif).subscribe(
                        saved -> LOG.info("[ChatNotification] Persisted in-app chat notification for user: {}", targetUserId),
                        err -> LOG.error("[ChatNotification] Failed to persist in-app chat notification: {}", err.getMessage())
                    );
                }
            } else {
                // Group / Team Chat Push Notification to all colleagues except author
                data.put("chatType", "group");
                data.put("channel", dto.getChannel() != null ? dto.getChannel() : "general");

                String title = senderName + " (Team Chat)";
                String body = contentPreview;
                if (pushNotificationService != null) {
                    pushNotificationService.sendGroupPushNotificationExcept(
                        dto.getUserId() != null ? dto.getUserId().toString() : "",
                        dto.getUserLogin(),
                        title,
                        body,
                        data
                    );
                }

                // MNC Standard: Persist in-app notification for all team members except author
                if (crmNotificationService != null && userRepository != null) {
                    userRepository.findAll()
                        .filter(u -> !Objects.equals(u.getId(), dto.getUserId()) && (dto.getUserLogin() == null || !u.getLogin().equalsIgnoreCase(dto.getUserLogin())))
                        .flatMap(colleague -> {
                            CrmNotification groupNotif = new CrmNotification();
                            groupNotif.setUserId(colleague.getLogin().toLowerCase());
                            groupNotif.setTitle(title);
                            groupNotif.setMessage(body);
                            groupNotif.setType("CHAT");
                            groupNotif.setReferenceId(dto.getId() != null ? dto.getId().toString() : null);
                            groupNotif.setReferenceType("CHAT_MESSAGE");
                            groupNotif.setIsRead(Boolean.FALSE);
                            groupNotif.setCreatedAt(Instant.now());
                            return crmNotificationService.createNotification(groupNotif);
                        })
                        .subscribe(
                            saved -> LOG.debug("[ChatNotification] Persisted group in-app chat notification for user: {}", saved.getUserId()),
                            err -> LOG.error("[ChatNotification] Failed to persist group in-app chat notification: {}", err.getMessage())
                        );
                }
            }
        } catch (Exception e) {
            LOG.error("[ChatPush] Error dispatching push notification for message: {}", e.getMessage());
        }
    }
}
