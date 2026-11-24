package com.salescrm.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.config.ApplicationProperties;
import com.salescrm.config.FacebookProperties;
import com.salescrm.domain.Customer;
import com.salescrm.domain.Lead;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import com.salescrm.repository.CustomerRepository;
import com.salescrm.repository.LeadRepository;
import com.salescrm.service.FirebasePushNotificationService;
import com.salescrm.service.InstagramService;
import com.salescrm.service.LeadActivityService;
import com.salescrm.service.dto.LeadActivityDTO;
import com.salescrm.service.dto.instagram.InstagramSendMessageRequestDTO;
import com.salescrm.service.dto.instagram.InstagramSendMessageResponseDTO;
import com.salescrm.service.dto.instagram.InstagramUserProfileDTO;
import com.salescrm.service.dto.instagram.InstagramWebhookPayloadDTO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Enterprise service implementation for Meta's Instagram Messaging API.
 * Handles webhook event processing, profile discovery, lead capture/updates,
 * CRM activity logging, push notification dispatch, and outbound DM responses.
 */
@Service
public class InstagramServiceImpl implements InstagramService {

    private static final Logger LOG = LoggerFactory.getLogger(InstagramServiceImpl.class);

    private final FacebookProperties facebookProperties;
    private final ApplicationProperties applicationProperties;
    private final CustomerRepository customerRepository;
    private final LeadRepository leadRepository;
    private final LeadActivityService leadActivityService;
    private final FirebasePushNotificationService pushNotificationService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Cache of processed message IDs (mid -> processed timestamp) to guarantee idempotency
    private final ConcurrentHashMap<String, Instant> processedMids = new ConcurrentHashMap<>();

    // User profile cache (igsid -> InstagramUserProfileDTO) to prevent redundant Graph API lookups
    private final ConcurrentHashMap<String, InstagramUserProfileDTO> profileCache = new ConcurrentHashMap<>();

    @Autowired
    public InstagramServiceImpl(
        FacebookProperties facebookProperties,
        ApplicationProperties applicationProperties,
        CustomerRepository customerRepository,
        LeadRepository leadRepository,
        LeadActivityService leadActivityService,
        @Autowired(required = false) FirebasePushNotificationService pushNotificationService,
        WebClient.Builder webClientBuilder
    ) {
        this.facebookProperties = facebookProperties;
        this.applicationProperties = applicationProperties;
        this.customerRepository = customerRepository;
        this.leadRepository = leadRepository;
        this.leadActivityService = leadActivityService;
        this.pushNotificationService = pushNotificationService;

        String baseUrl = facebookProperties.getGraphApiBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://graph.facebook.com";
        }
        this.webClient = webClientBuilder
            .baseUrl(baseUrl)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    private String getEffectiveAccessToken() {
        if (applicationProperties.getInstagram() != null &&
            applicationProperties.getInstagram().getPageAccessToken() != null &&
            !applicationProperties.getInstagram().getPageAccessToken().isBlank()) {
            return applicationProperties.getInstagram().getPageAccessToken().trim();
        }
        return facebookProperties.getPageAccessToken() != null ? facebookProperties.getPageAccessToken().trim() : "";
    }

    private String getEffectiveVerifyToken() {
        if (applicationProperties.getInstagram() != null &&
            applicationProperties.getInstagram().getVerifyToken() != null &&
            !applicationProperties.getInstagram().getVerifyToken().isBlank()) {
            return applicationProperties.getInstagram().getVerifyToken().trim();
        }
        return facebookProperties.getVerifyToken() != null ? facebookProperties.getVerifyToken().trim() : "";
    }

    private String getGraphApiVersion() {
        return (facebookProperties.getGraphApiVersion() != null && !facebookProperties.getGraphApiVersion().isBlank())
            ? facebookProperties.getGraphApiVersion().trim()
            : "v21.0";
    }

    @Override
    public boolean verifySubscriptionToken(String mode, String token) {
        if (!"subscribe".equalsIgnoreCase(mode)) {
            LOG.warn("Instagram webhook verification rejected: mode is '{}', expected 'subscribe'", mode);
            return false;
        }

        String configuredToken = getEffectiveVerifyToken();
        if (configuredToken.isBlank()) {
            LOG.error("Instagram webhook verification failed: verify token is not configured");
            return false;
        }

        boolean isValid = MessageDigest.isEqual(
            configuredToken.getBytes(StandardCharsets.UTF_8),
            (token != null ? token : "").getBytes(StandardCharsets.UTF_8)
        );

        if (!isValid) {
            LOG.warn("Instagram webhook verification failed: token mismatch");
        }
        return isValid;
    }

    @Override
    public Mono<Void> processWebhookPayload(InstagramWebhookPayloadDTO payload) {
        if (payload == null || payload.getEntry() == null || payload.getEntry().isEmpty()) {
            return Mono.empty();
        }

        return Flux.fromIterable(payload.getEntry())
            .flatMap(entry -> {
                String pageOrAccountId = entry.getId();
                if (entry.getMessaging() == null || entry.getMessaging().isEmpty()) {
                    return Flux.empty();
                }
                return Flux.fromIterable(entry.getMessaging())
                    .flatMap(messaging -> handleSingleMessage(messaging, pageOrAccountId));
            })
            .then();
    }

    private Mono<Void> handleSingleMessage(InstagramWebhookPayloadDTO.Messaging messaging, String pageOrAccountId) {
        if (messaging == null || messaging.getMessage() == null) {
            return Mono.empty();
        }

        // Ignore echo messages (messages sent by the business page/account itself)
        if (Boolean.TRUE.equals(messaging.getMessage().getIsEcho())) {
            LOG.debug("Skipping Instagram echo message sent by page");
            return Mono.empty();
        }

        String mid = messaging.getMessage().getMid();
        String text = messaging.getMessage().getText();
        String senderId = (messaging.getSender() != null) ? messaging.getSender().getId() : null;
        Long timestamp = messaging.getTimestamp();

        if (senderId == null || senderId.isBlank()) {
            LOG.warn("Skipping Instagram message with empty senderId");
            return Mono.empty();
        }

        if (text == null || text.isBlank()) {
            // Check if there are attachments
            if (messaging.getMessage().getAttachments() != null && !messaging.getMessage().getAttachments().isEmpty()) {
                text = "[Attachment: " + messaging.getMessage().getAttachments().get(0).getType() + "]";
            } else {
                LOG.debug("Skipping Instagram message with empty text and no attachments");
                return Mono.empty();
            }
        }

        // Deduplication using message ID (mid)
        if (mid != null && !mid.isBlank()) {
            Instant existing = processedMids.putIfAbsent(mid, Instant.now());
            if (existing != null) {
                LOG.info("Ignoring duplicate Instagram message mid: {}", mid);
                return Mono.empty();
            }
        }

        final String incomingText = text.trim();
        final Instant messageTime = (timestamp != null && timestamp > 0)
            ? Instant.ofEpochMilli(timestamp)
            : Instant.now();

        LOG.info("Processing inbound Instagram DM: senderId={}, text='{}'", senderId, incomingText);

        return getUserProfile(senderId)
            .flatMap(profile -> processLeadForUser(senderId, profile, incomingText, messageTime, pageOrAccountId));
    }

    private Mono<Void> processLeadForUser(
        String senderId,
        InstagramUserProfileDTO profile,
        String text,
        Instant messageTime,
        String pageOrAccountId
    ) {
        String username = (profile.getUsername() != null && !profile.getUsername().isBlank())
            ? profile.getUsername().trim()
            : senderId;
        String displayName = (profile.getName() != null && !profile.getName().isBlank())
            ? profile.getName().trim()
            : "@" + username;

        String externalId = "ig_" + senderId;

        return leadRepository.findByExternalId(externalId)
            .flatMap(existingLead -> updateExistingLead(existingLead, username, displayName, text, senderId))
            .switchIfEmpty(Mono.defer(() -> createNewLeadFromInstagram(senderId, username, displayName, text, messageTime, pageOrAccountId)))
            .then();
    }

    private Mono<Lead> updateExistingLead(Lead lead, String username, String displayName, String text, String senderId) {
        String updatedNotes = (lead.getNotes() != null ? lead.getNotes() : "") +
            "\n[" + Instant.now() + "] @" + username + ": " + text;
        lead.setNotes(updatedNotes);
        lead.setUpdatedAt(Instant.now());

        return leadRepository.save(lead)
            .flatMap(savedLead -> {
                // 1. Log Activity
                LeadActivityDTO activity = new LeadActivityDTO();
                activity.setLeadId(savedLead.getId());
                activity.setUserId("instagram");
                activity.setUserName("Instagram DM");
                activity.setActionType("OUTREACH");
                activity.setSummary("Instagram DM from @" + username);
                activity.setDetails(text);
                activity.setCreatedAt(Instant.now());

                return leadActivityService.logActivity(activity)
                    .defaultIfEmpty(null)
                    .doOnSuccess(ignored -> {
                        // 2. Dispatch Push Notification
                        if (pushNotificationService != null) {
                            Map<String, String> data = new HashMap<>();
                            data.put("leadId", String.valueOf(savedLead.getId()));
                            data.put("platform", "IG");
                            data.put("senderId", senderId);
                            data.put("username", username);
                            pushNotificationService.sendPushNotificationToAll(
                                "Instagram DM from @" + username,
                                text,
                                data
                            );
                        }
                    })
                    .thenReturn(savedLead);
            });
    }

    private Mono<Lead> createNewLeadFromInstagram(
        String senderId,
        String username,
        String displayName,
        String text,
        Instant messageTime,
        String pageOrAccountId
    ) {
        String phoneKey = "IG:" + senderId;

        return customerRepository.findByPhoneNumber(phoneKey)
            .switchIfEmpty(Mono.defer(() -> {
                Customer newCustomer = new Customer();
                newCustomer.setFullName(displayName);
                newCustomer.setPhoneNumber(phoneKey);
                newCustomer.setRawPhoneNumber("@" + username);
                newCustomer.setLocation("Instagram");
                newCustomer.setCreatedAt(Instant.now());
                return customerRepository.save(newCustomer);
            }))
            .flatMap(customer -> {
                Lead newLead = new Lead();
                newLead.setExternalId("ig_" + senderId);
                newLead.setCreatedTime(messageTime != null ? messageTime : Instant.now());
                newLead.setIsOrganic(true);
                newLead.setPlatform(PlatformType.IG);
                newLead.setStatus(LeadStatus.CREATED);
                newLead.setMetaPageId(pageOrAccountId);
                newLead.setCustomer(customer);
                newLead.setCustomerId(customer.getId());
                newLead.setNotes("Captured via Instagram Direct Message\nHandle: @" + username + "\nMessage: " + text);
                newLead.setCreatedAt(Instant.now());
                newLead.setUpdatedAt(Instant.now());

                return leadRepository.save(newLead)
                    .flatMap(savedLead -> {
                        LOG.info("Created new CRM Lead from Instagram DM: ID={}, externalId={}, username=@{}",
                            savedLead.getId(), savedLead.getExternalId(), username);

                        LeadActivityDTO activity = new LeadActivityDTO();
                        activity.setLeadId(savedLead.getId());
                        activity.setUserId("instagram");
                        activity.setUserName("Instagram DM");
                        activity.setActionType("OUTREACH");
                        activity.setSummary("New Lead via Instagram DM (@" + username + ")");
                        activity.setDetails(text);
                        activity.setCreatedAt(Instant.now());

                        return leadActivityService.logActivity(activity)
                            .defaultIfEmpty(null)
                            .doOnSuccess(ignored -> {
                                if (pushNotificationService != null) {
                                    Map<String, String> data = new HashMap<>();
                                    data.put("leadId", String.valueOf(savedLead.getId()));
                                    data.put("platform", "IG");
                                    data.put("senderId", senderId);
                                    data.put("username", username);
                                    pushNotificationService.sendPushNotificationToAll(
                                        "New Instagram Lead: @" + username,
                                        text,
                                        data
                                    );
                                }
                            })
                            .thenReturn(savedLead);
                    });
            });
    }

    @Override
    public Mono<InstagramUserProfileDTO> getUserProfile(String igsid) {
        if (igsid == null || igsid.isBlank()) {
            return Mono.just(createFallbackProfile("unknown"));
        }

        InstagramUserProfileDTO cached = profileCache.get(igsid);
        if (cached != null) {
            return Mono.just(cached);
        }

        String accessToken = getEffectiveAccessToken();
        if (accessToken.isBlank()) {
            LOG.warn("Cannot query Instagram Graph API user profile: Access Token not configured");
            InstagramUserProfileDTO fallback = createFallbackProfile(igsid);
            profileCache.put(igsid, fallback);
            return Mono.just(fallback);
        }

        String path = String.format("/%s/%s", getGraphApiVersion(), igsid);

        return webClient.get()
            .uri(uriBuilder -> uriBuilder
                .path(path)
                .queryParam("fields", "name,username,profile_pic")
                .queryParam("access_token", accessToken)
                .build()
            )
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(10))
            .map(body -> {
                try {
                    InstagramUserProfileDTO dto = objectMapper.readValue(body, InstagramUserProfileDTO.class);
                    if (dto.getUsername() == null || dto.getUsername().isBlank()) {
                        dto.setUsername(igsid);
                    }
                    profileCache.put(igsid, dto);
                    return dto;
                } catch (Exception e) {
                    LOG.warn("Failed to parse Instagram user profile: {}", e.getMessage());
                    return createFallbackProfile(igsid);
                }
            })
            .onErrorResume(err -> {
                LOG.warn("Could not fetch Instagram profile for igsid={}: {}", igsid, err.getMessage());
                InstagramUserProfileDTO fallback = createFallbackProfile(igsid);
                profileCache.put(igsid, fallback);
                return Mono.just(fallback);
            });
    }

    private InstagramUserProfileDTO createFallbackProfile(String igsid) {
        InstagramUserProfileDTO dto = new InstagramUserProfileDTO();
        dto.setId(igsid);
        dto.setUsername(igsid);
        dto.setName("Instagram User");
        return dto;
    }

    @Override
    public Mono<InstagramSendMessageResponseDTO> sendMessage(InstagramSendMessageRequestDTO request) {
        if (request == null || request.getRecipientId() == null || request.getRecipientId().isBlank()) {
            return Mono.just(InstagramSendMessageResponseDTO.error("recipientId is required"));
        }
        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return Mono.just(InstagramSendMessageResponseDTO.error("message text is required"));
        }

        String accessToken = getEffectiveAccessToken();
        if (accessToken.isBlank()) {
            return Mono.just(InstagramSendMessageResponseDTO.error("Meta/Instagram Page Access Token is not configured"));
        }

        String path = String.format("/%s/me/messages", getGraphApiVersion());

        Map<String, Object> body = Map.of(
            "recipient", Map.of("id", request.getRecipientId().trim()),
            "message", Map.of("text", request.getMessage().trim())
        );

        LOG.info("Sending outbound Instagram DM to recipientId={}", request.getRecipientId());

        return webClient.post()
            .uri(uriBuilder -> uriBuilder.path(path).queryParam("access_token", accessToken).build())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .timeout(Duration.ofSeconds(15))
            .flatMap(res -> {
                try {
                    JsonNode node = objectMapper.readTree(res);
                    String messageId = node.path("message_id").asText("");
                    String recipientId = node.path("recipient_id").asText(request.getRecipientId());

                    InstagramSendMessageResponseDTO responseDTO = new InstagramSendMessageResponseDTO(recipientId, messageId);

                    // Log CRM Lead Activity if leadId was provided
                    if (request.getLeadId() != null) {
                        LeadActivityDTO act = new LeadActivityDTO();
                        act.setLeadId(request.getLeadId());
                        act.setUserId("agent");
                        act.setUserName("CRM Agent");
                        act.setActionType("OUTREACH");
                        act.setSummary("Sent Instagram DM Reply");
                        act.setDetails(request.getMessage().trim());
                        act.setCreatedAt(Instant.now());
                        return leadActivityService.logActivity(act)
                            .defaultIfEmpty(null)
                            .thenReturn(responseDTO);
                    }

                    return Mono.just(responseDTO);
                } catch (Exception e) {
                    LOG.error("Failed to parse Meta send message response: {}", e.getMessage());
                    return Mono.just(InstagramSendMessageResponseDTO.error("Sent, but failed to parse response: " + e.getMessage()));
                }
            })
            .onErrorResume(err -> {
                LOG.error("Failed to send Instagram DM: {}", err.getMessage());
                return Mono.just(InstagramSendMessageResponseDTO.error("Failed to send Instagram DM: " + err.getMessage()));
            });
    }

    @Override
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        String token = getEffectiveAccessToken();
        status.put("configured", !token.isBlank());
        status.put("tokenConfigured", !token.isBlank());
        status.put("verifyTokenConfigured", !getEffectiveVerifyToken().isBlank());
        status.put("webhookUrl", "https://ssasaleshub.cloud/api/webhooks/instagram");
        status.put("graphApiVersion", getGraphApiVersion());
        status.put("cachedProfilesCount", profileCache.size());
        status.put("processedMessagesCount", processedMids.size());
        return status;
    }
}
