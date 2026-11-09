package com.salescrm.service;

import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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

@Service
public class FirebasePushNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(FirebasePushNotificationService.class);
    private static final Path TOKENS_FILE = Path.of("fcm_device_tokens.properties");

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    // Resilient device token cache (userId -> Set of fcmTokens)
    private final Map<String, Set<String>> userTokens = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
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
        if (userId != null && token != null && !token.isBlank()) {
            userTokens.computeIfAbsent(userId.toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(token);
            userTokens.computeIfAbsent("default", k -> ConcurrentHashMap.newKeySet()).add(token);
            persistTokens();
            LOG.info("Registered and persisted FCM token for user: {} (token={})", userId, maskToken(token));
        }
    }

    public void removeToken(String token) {
        if (token == null || token.isBlank()) return;
        boolean changed = false;
        for (Set<String> tokens : userTokens.values()) {
            if (tokens.remove(token)) {
                changed = true;
            }
        }
        if (changed) {
            persistTokens();
            LOG.info("Removed unregistered FCM token: {}", maskToken(token));
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
        Set<String> tokens = getTokens(userId);
        if (tokens.isEmpty()) {
            LOG.warn("No active FCM device token available for user: {}. Skipping push notification.", userId);
            return;
        }

        if (firebaseMessaging == null) {
            LOG.warn("FirebaseMessaging is not initialized. Skipping push notification.");
            return;
        }

        for (String token : tokens) {
            sendDirectPush(token, userId, title, body, data);
        }
    }

    public void sendPushNotificationToAll(String title, String body, Map<String, String> data) {
        Set<String> uniqueTokens = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : userTokens.entrySet()) {
            if ("default".equalsIgnoreCase(entry.getKey())) continue;
            if (entry.getValue() != null) {
                uniqueTokens.addAll(entry.getValue());
            }
        }
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
            LOG.warn("No FCM device token available for recipient id={} login={}. Skipping chat push.", recipientId, recipientLogin);
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
            Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

            AndroidConfig androidConfig = AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(
                    AndroidNotification.builder()
                        .setChannelId("fcm_default_channel")
                        .setIcon("ic_notification")
                        .setColor("#029858")
                        .setSound("default")
                        .setDefaultSound(true)
                        .setDefaultVibrateTimings(true)
                        .setPriority(AndroidNotification.Priority.HIGH)
                        .setVisibility(AndroidNotification.Visibility.PUBLIC)
                        .build()
                )
                .build();

            WebpushConfig webpushConfig = WebpushConfig.builder()
                .setNotification(
                    WebpushNotification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .setIcon("/favicon.png")
                        .build()
                )
                .putData("route", data != null && data.containsKey("route") ? data.get("route") : "")
                .build();

            Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .setAndroidConfig(androidConfig)
                .setWebpushConfig(webpushConfig);

            Map<String, String> payloadData = data != null ? new HashMap<>(data) : new HashMap<>();
            payloadData.putIfAbsent("title", title);
            payloadData.putIfAbsent("body", body);
            messageBuilder.putAllData(payloadData);

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

    private String maskToken(String token) {
        if (token == null || token.length() <= 12) return "***";
        return token.substring(0, 6) + "..." + token.substring(token.length() - 6);
    }
}
