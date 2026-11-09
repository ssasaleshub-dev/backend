package com.salescrm.service;

import com.google.firebase.messaging.*;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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

    // Resilient device token cache (userId -> fcmToken)
    private final Map<String, String> userTokens = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        try {
            if (Files.exists(TOKENS_FILE)) {
                Properties props = new Properties();
                try (InputStream in = Files.newInputStream(TOKENS_FILE)) {
                    props.load(in);
                    for (String key : props.stringPropertyNames()) {
                        userTokens.put(key.toLowerCase(), props.getProperty(key));
                    }
                }
                LOG.info("Loaded {} persisted FCM tokens from disk ({})", userTokens.size(), TOKENS_FILE);
            }
        } catch (Exception e) {
            LOG.warn("Failed to load persisted FCM tokens: {}", e.getMessage());
        }
    }

    private synchronized void persistTokens() {
        try {
            Properties props = new Properties();
            props.putAll(userTokens);
            try (OutputStream out = Files.newOutputStream(TOKENS_FILE)) {
                props.store(out, "SSA CRM FCM Device Tokens");
            }
        } catch (Exception e) {
            LOG.warn("Failed to persist FCM tokens to disk: {}", e.getMessage());
        }
    }

    public void registerToken(String userId, String token) {
        if (userId != null && token != null) {
            userTokens.put(userId.toLowerCase(), token);
            userTokens.put("default", token);
            persistTokens();
            LOG.info("Registered and persisted FCM token for user: {}", userId);
        }
    }

    public String getToken(String userId) {
        if (userId != null) {
            String t = userTokens.get(userId.toLowerCase());
            if (t != null) return t;
        }
        return userTokens.get("default");
    }

    public void sendPushNotification(String userId, String title, String body, Map<String, String> data) {
        String token = getToken(userId);
        if (token == null) {
            LOG.warn("No active FCM device token available for user: {}. Skipping push notification.", userId);
            return;
        }

        if (firebaseMessaging == null) {
            LOG.warn("FirebaseMessaging is not initialized. Skipping push notification.");
            return;
        }

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

            Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .setAndroidConfig(androidConfig);

            Map<String, String> payloadData = data != null ? new HashMap<>(data) : new HashMap<>();
            payloadData.putIfAbsent("title", title);
            payloadData.putIfAbsent("body", body);
            messageBuilder.putAllData(payloadData);

            String response = firebaseMessaging.send(messageBuilder.build());
            LOG.info("FCM push notification sent successfully to user {}: messageId={}", userId, response);
        } catch (FirebaseMessagingException e) {
            LOG.error("Failed to send FCM push notification to user {}: {}", userId, e.getMessage());
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                userTokens.remove(userId.toLowerCase());
                persistTokens();
            }
        } catch (Exception e) {
            LOG.error("Unexpected error sending push notification: {}", e.getMessage());
        }
    }

    public void sendPushNotificationToAll(String title, String body, Map<String, String> data) {
        for (Map.Entry<String, String> entry : userTokens.entrySet()) {
            if ("default".equalsIgnoreCase(entry.getKey())) continue;
            sendDirectPush(entry.getValue(), entry.getKey(), title, body, data);
        }
    }

    private void sendDirectPush(String token, String userId, String title, String body, Map<String, String> data) {
        if (firebaseMessaging == null || token == null) return;
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

            Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .setAndroidConfig(androidConfig);

            Map<String, String> payloadData = data != null ? new HashMap<>(data) : new HashMap<>();
            payloadData.putIfAbsent("title", title);
            payloadData.putIfAbsent("body", body);
            messageBuilder.putAllData(payloadData);

            String response = firebaseMessaging.send(messageBuilder.build());
            LOG.info("FCM push sent to user {}: messageId={}", userId, response);
        } catch (Exception e) {
            LOG.error("Failed to send direct FCM push: {}", e.getMessage());
        }
    }
}
