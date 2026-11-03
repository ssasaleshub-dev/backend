package com.salescrm.service;

import com.google.firebase.messaging.*;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FirebasePushNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(FirebasePushNotificationService.class);

    @Autowired(required = false)
    private FirebaseMessaging firebaseMessaging;

    // In-memory token cache (userId -> fcmToken)
    private final Map<String, String> userTokens = new ConcurrentHashMap<>();

    public void registerToken(String userId, String token) {
        if (userId != null && token != null) {
            userTokens.put(userId.toLowerCase(), token);
            LOG.info("Registered FCM token for user: {}", userId);
        }
    }

    public String getToken(String userId) {
        if (userId == null) return null;
        return userTokens.get(userId.toLowerCase());
    }

    public void sendPushNotification(String userId, String title, String body, Map<String, String> data) {
        String token = getToken(userId);
        if (token == null) {
            LOG.debug("No active FCM token found for user: {}", userId);
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
                        .setSound("default")
                        .setDefaultVibrateTimings(true)
                        .build()
                )
                .build();

            Message.Builder messageBuilder = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .setAndroidConfig(androidConfig);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            String response = firebaseMessaging.send(messageBuilder.build());
            LOG.info("FCM push notification sent successfully to user {}: messageId={}", userId, response);
        } catch (FirebaseMessagingException e) {
            LOG.error("Failed to send FCM push notification to user {}: {}", userId, e.getMessage());
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                userTokens.remove(userId.toLowerCase());
            }
        } catch (Exception e) {
            LOG.error("Unexpected error sending push notification: {}", e.getMessage());
        }
    }
    public void sendPushNotificationToAll(String title, String body, Map<String, String> data) {
        for (String user : userTokens.keySet()) {
            sendPushNotification(user, title, body, data);
        }
    }
}
