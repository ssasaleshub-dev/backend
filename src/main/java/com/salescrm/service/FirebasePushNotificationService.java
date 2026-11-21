package com.salescrm.service;

import com.google.firebase.messaging.*;
import com.salescrm.domain.UserDeviceToken;
import com.salescrm.repository.UserDeviceTokenRepository;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class FirebasePushNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(FirebasePushNotificationService.class);
    private static final Path TOKENS_FILE = Path.of("fcm_device_tokens.properties");

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    @Autowired(required = false)
    private UserDeviceTokenRepository userDeviceTokenRepository;

    // Resilient device token cache (userId -> Set of fcmTokens)
    private final Map<String, Set<String>> userTokens = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        // 1. Load from PostgreSQL table if available
        if (userDeviceTokenRepository != null) {
            try {
                userDeviceTokenRepository.findAll()
                    .doOnNext(udt -> {
                        if (udt.getUserId() != null && udt.getToken() != null && !udt.getToken().isBlank()) {
                            userTokens.computeIfAbsent(udt.getUserId().toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(udt.getToken().trim());
                            userTokens.computeIfAbsent("default", k -> ConcurrentHashMap.newKeySet()).add(udt.getToken().trim());
                        }
                    })
                    .doOnComplete(() -> LOG.info("Loaded persisted FCM device tokens from PostgreSQL user_device_tokens table"))
                    .doOnError(err -> LOG.warn("Could not load tokens from user_device_tokens table yet: {}", err.getMessage()))
                    .subscribe();
            } catch (Exception e) {
                LOG.warn("Failed to initialize tokens from DB: {}", e.getMessage());
            }
        }

        // 2. Load from disk properties fallback
        try {
            if (Files.exists(TOKENS_FILE)) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(TOKENS_FILE)) {
                    props.load(in);
                    for (String key : props.stringPropertyNames()) {
                        String val = props.getProperty(key);
                        if (val != null && !val.isBlank()) {
                            Set<String> tokens = userTokens.computeIfAbsent(key.toLowerCase(), k -> ConcurrentHashMap.newKeySet());
                            for (String t : val.split(",")) {
                                if (!t.trim().isEmpty()) {
                                    tokens.add(t.trim());
                                }
                            }
                        }
                    }
                }
                LOG.info("Loaded persisted FCM tokens from disk ({})", TOKENS_FILE);
            }
        } catch (Exception e) {
            LOG.warn("Failed to load persisted FCM tokens: {}", e.getMessage());
        }
    }

    private synchronized void persistTokens() {
        try {
            Properties props = new Properties();
            for (Map.Entry<String, Set<String>> entry : userTokens.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    props.setProperty(entry.getKey(), String.join(",", entry.getValue()));
                }
            }
            try (OutputStream out = Files.newOutputStream(TOKENS_FILE)) {
                props.store(out, "SSA CRM FCM Device Tokens");
            }
        } catch (Exception e) {
            LOG.warn("Failed to persist FCM tokens to disk: {}", e.getMessage());
        }
    }

    public void registerToken(String userId, String token) {
        registerToken(userId, token, null);
    }

    public void registerToken(String userId, String token, String deviceType) {
        if (userId != null && token != null && !token.isBlank()) {
            final String cleanUserId = userId.toLowerCase().trim();
            final String cleanToken = token.trim();

            userTokens.computeIfAbsent(cleanUserId, k -> ConcurrentHashMap.newKeySet()).add(cleanToken);
            userTokens.computeIfAbsent("default", k -> ConcurrentHashMap.newKeySet()).add(cleanToken);
            persistTokens();
            LOG.info("Registered and cached FCM token for user: {} (token={})", cleanUserId, maskToken(cleanToken));

            // Persist to PostgreSQL user_device_tokens table
            if (userDeviceTokenRepository != null) {
                userDeviceTokenRepository.findByToken(cleanToken)
                    .flatMap(existing -> {
                        existing.setUserId(cleanUserId);
                        if (deviceType != null && !deviceType.isBlank()) {
                            existing.setDeviceType(deviceType.trim().toUpperCase());
                        }
                        existing.setUpdatedAt(Instant.now());
                        return userDeviceTokenRepository.save(existing);
                    })
                    .switchIfEmpty(Mono.defer(() -> {
                        UserDeviceToken newToken = new UserDeviceToken();
                        newToken.setUserId(cleanUserId);
                        newToken.setToken(cleanToken);
                        newToken.setDeviceType(deviceType != null ? deviceType.trim().toUpperCase() : null);
                        newToken.setCreatedAt(Instant.now());
                        newToken.setUpdatedAt(Instant.now());
                        return userDeviceTokenRepository.save(newToken);
                    }))
                    .subscribe(
                        saved -> LOG.info("Successfully persisted device token into user_device_tokens table for user: {}", cleanUserId),
                        err -> LOG.warn("Could not save token into user_device_tokens table: {}", err.getMessage())
                    );
            }
        }
    }

    public void removeToken(String token) {
        if (token == null || token.isBlank()) return;
        final String cleanToken = token.trim();
        boolean changed = false;
        for (Set<String> tokens : userTokens.values()) {
            if (tokens.remove(cleanToken)) {
                changed = true;
            }
        }
        if (changed) {
            persistTokens();
            LOG.info("Removed unregistered FCM token from memory cache: {}", maskToken(cleanToken));
        }

        // Delete from PostgreSQL user_device_tokens table
        if (userDeviceTokenRepository != null) {
            userDeviceTokenRepository.deleteByToken(cleanToken)
                .subscribe(
                    deletedCount -> LOG.info("Deleted unregistered token from user_device_tokens table: count={}", deletedCount),
                    err -> LOG.warn("Could not delete token from user_device_tokens table: {}", err.getMessage())
                );
        }
    }

    public String getToken(String userId) {
        Set<String> tokens = getTokens(userId);
        return tokens.isEmpty() ? null : tokens.iterator().next();
    }

    public Set<String> getTokens(String userId) {
        Set<String> result = new HashSet<>();
        if (userId != null) {
            Set<String> t = userTokens.get(userId.toLowerCase());
            if (t != null) result.addAll(t);
        }
        if (result.isEmpty()) {
            Set<String> def = userTokens.get("default");
            if (def != null) result.addAll(def);
        }
        return result;
    }

    public void sendPushNotification(String userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null) {
            LOG.warn("FirebaseMessaging is not initialized. Skipping push notification.");
            return;
        }

        Set<String> tokens = getTokens(userId);
        if (tokens.isEmpty()) {
            LOG.warn("No active FCM device token available in cache for user: {}. Checking DB fallback.", userId);
            if (userDeviceTokenRepository != null && userId != null) {
                userDeviceTokenRepository.findAllByUserId(userId.toLowerCase())
                    .map(UserDeviceToken::getToken)
                    .filter(t -> t != null && !t.isBlank())
                    .subscribe(dbToken -> {
                        String clean = dbToken.trim();
                        userTokens.computeIfAbsent(userId.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(clean);
                        sendDirectPush(clean, userId, title, body, data);
                    });
            }
            return;
        }

        for (String token : tokens) {
            sendDirectPush(token, userId, title, body, data);
        }
    }

    public void sendPushNotificationToAll(String title, String body, Map<String, String> data) {
        Set<String> uniqueTokens = new HashSet<>();
        for (Set<String> tokens : userTokens.values()) {
            if (tokens != null) {
                for (String t : tokens) {
                    if (t != null && !t.isBlank()) {
                        uniqueTokens.add(t.trim());
                    }
                }
            }
        }
        LOG.info("Broadcasting push notification to {} registered device(s): '{}'", uniqueTokens.size(), title);
        for (String token : uniqueTokens) {
            sendDirectPush(token, "all_users", title, body, data);
        }
    }

    /**
     * Send push notification for P2P direct chat message to recipient.
     */
    public void sendChatPushNotification(String recipientId, String recipientLogin, String title, String body, Map<String, String> data) {
        Set<String> tokens = new HashSet<>();
        if (recipientId != null) {
            Set<String> t = userTokens.get(recipientId.toLowerCase());
            if (t != null) tokens.addAll(t);
        }
        if (recipientLogin != null) {
            Set<String> t = userTokens.get(recipientLogin.toLowerCase());
            if (t != null) tokens.addAll(t);
        }
        if (tokens.isEmpty()) {
            Set<String> def = userTokens.get("default");
            if (def != null) tokens.addAll(def);
        }

        if (tokens.isEmpty()) {
            LOG.warn("No FCM device token available in cache for recipient id={} login={}. Checking DB fallback.", recipientId, recipientLogin);
            if (userDeviceTokenRepository != null) {
                Flux.concat(
                    recipientLogin != null ? userDeviceTokenRepository.findAllByUserId(recipientLogin.toLowerCase()) : Flux.empty(),
                    recipientId != null ? userDeviceTokenRepository.findAllByUserId(recipientId.toLowerCase()) : Flux.empty()
                )
                .map(UserDeviceToken::getToken)
                .filter(t -> t != null && !t.isBlank())
                .subscribe(dbToken -> {
                    String clean = dbToken.trim();
                    if (recipientLogin != null) {
                        userTokens.computeIfAbsent(recipientLogin.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(clean);
                    }
                    sendDirectPush(clean, recipientLogin != null ? recipientLogin : recipientId, title, body, data);
                });
            }
            return;
        }

        LOG.info("Sending P2P chat push notification to recipient id={} login={} across {} device(s)", recipientId, recipientLogin, tokens.size());
        for (String token : tokens) {
            sendDirectPush(token, recipientLogin != null ? recipientLogin : recipientId, title, body, data);
        }
    }

    /**
     * Send push notification for Team/Group chat message to all registered team members EXCEPT the sender.
     */
    public void sendGroupPushNotificationExcept(String senderId, String senderLogin, String title, String body, Map<String, String> data) {
        String cleanSenderId = senderId != null ? senderId.toLowerCase() : "";
        String cleanSenderLogin = senderLogin != null ? senderLogin.toLowerCase() : "";

        Set<String> senderTokens = new HashSet<>();
        if (!cleanSenderId.isEmpty() && userTokens.containsKey(cleanSenderId)) {
            senderTokens.addAll(userTokens.get(cleanSenderId));
        }
        if (!cleanSenderLogin.isEmpty() && userTokens.containsKey(cleanSenderLogin)) {
            senderTokens.addAll(userTokens.get(cleanSenderLogin));
        }

        Set<String> targetTokens = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : userTokens.entrySet()) {
            String key = entry.getKey();
            if ("default".equalsIgnoreCase(key)) continue;
            if (key.equalsIgnoreCase(cleanSenderId) || key.equalsIgnoreCase(cleanSenderLogin)) {
                continue; // Do not send notification to the message author!
            }
            if (entry.getValue() != null) {
                for (String t : entry.getValue()) {
                    if (t != null && !t.isBlank() && !senderTokens.contains(t)) {
                        targetTokens.add(t);
                    }
                }
            }
        }

        // Fallback: If only default token exists and not sender's token
        if (targetTokens.isEmpty() && userTokens.containsKey("default")) {
            for (String dt : userTokens.get("default")) {
                if (!senderTokens.contains(dt)) {
                    targetTokens.add(dt);
                }
            }
        }

        LOG.info("Dispatching Team Chat push notification to {} device(s) (sender={})", targetTokens.size(), senderLogin);
        for (String token : targetTokens) {
            sendDirectPush(token, "group_member", title, body, data);
        }
    }

    private void sendDirectPush(String token, String userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null || token == null || token.isBlank()) return;
        try {
            String cleanTitle = cleanText(title);
            String cleanBody = cleanText(body);

            String collapseTag = null;
            if (data != null) {
                if (data.containsKey("leadId") && !data.get("leadId").isBlank()) {
                    collapseTag = "lead_" + data.get("leadId").trim();
                } else if (data.containsKey("messageId") && !data.get("messageId").isBlank()) {
                    collapseTag = "chat_" + data.get("messageId").trim();
                }
            }

            Notification notification = Notification.builder()
                .setTitle(cleanTitle)
                .setBody(cleanBody)
                .build();

            AndroidNotification.Builder androidNotificationBuilder = AndroidNotification.builder()
                .setChannelId("fcm_default_channel")
                .setSound("default")
                .setDefaultSound(true)
                .setDefaultVibrateTimings(true)
                .setPriority(AndroidNotification.Priority.HIGH)
                .setVisibility(AndroidNotification.Visibility.PUBLIC);

            if (collapseTag != null) {
                androidNotificationBuilder.setTag(collapseTag);
            }

            AndroidConfig.Builder androidConfigBuilder = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(androidNotificationBuilder.build());

            if (collapseTag != null) {
                androidConfigBuilder.setCollapseKey(collapseTag);
            }

            WebpushNotification.Builder webpushBuilder = WebpushNotification.builder()
                .setTitle(cleanTitle)
                .setBody(cleanBody)
                .setIcon("/favicon.ico")
                .setBadge("/favicon.ico");

            if (collapseTag != null) {
                webpushBuilder.setTag(collapseTag);
            }

            Map<String, String> payloadData = data != null ? new HashMap<>(data) : new HashMap<>();
            payloadData.putIfAbsent("title", cleanTitle);
            payloadData.putIfAbsent("body", cleanBody);

            WebpushConfig webpushConfig = WebpushConfig.builder()
                .setNotification(webpushBuilder.build())
                .putAllData(payloadData)
                .build();

            Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .setAndroidConfig(androidConfigBuilder.build())
                .setWebpushConfig(webpushConfig)
                .putAllData(payloadData);

            String response = firebaseMessaging.send(messageBuilder.build());
            LOG.info("FCM push sent to user {}: messageId={}", userId, response);
        } catch (FirebaseMessagingException e) {
            LOG.error("Failed to send direct FCM push to user {}: {}", userId, e.getMessage());
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                removeToken(token);
            }
        } catch (Exception e) {
            LOG.error("Failed to send direct FCM push: {}", e.getMessage());
        }
    }

    private String cleanText(String text) {
        if (text == null) return "";
        // Strip emojis and symbols to ensure clean professional notifications
        return text.replaceAll("[\\p{So}\\p{Cn}\\uD83C-\\uDBFF\\uDC00-\\uDFFF]", "").trim();
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 12) return "***";
        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }
}
