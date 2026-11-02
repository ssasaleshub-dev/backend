package com.salescrm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class FirebaseConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseConfiguration.class);

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = null;

                // 1. Check Base64 environment variable
                String base64Key = System.getenv("FIREBASE_CREDENTIALS_BASE64");
                if (base64Key != null && !base64Key.isBlank()) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(base64Key.trim());
                        serviceAccount = new ByteArrayInputStream(decoded);
                        LOG.info("Loaded Firebase credentials from FIREBASE_CREDENTIALS_BASE64 environment variable");
                    } catch (Exception e) {
                        LOG.warn("Failed to decode FIREBASE_CREDENTIALS_BASE64: {}", e.getMessage());
                    }
                }

                // 2. Check path environment variable
                if (serviceAccount == null) {
                    String envPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
                    if (envPath != null && !envPath.isBlank()) {
                        Path path = Path.of(envPath);
                        if (Files.exists(path)) {
                            serviceAccount = Files.newInputStream(path);
                            LOG.info("Loaded Firebase credentials from path: {}", envPath);
                        }
                    }
                }

                // 3. Check well-known filesystem paths
                if (serviceAccount == null) {
                    String[] candidates = {
                        "/opt/java-backend/firebase-service-account.json",
                        "/root/apps/backend/firebase-service-account.json",
                        "firebase-service-account.json",
                    };
                    for (String candidate : candidates) {
                        Path path = Path.of(candidate);
                        if (Files.exists(path)) {
                            serviceAccount = Files.newInputStream(path);
                            LOG.info("Loaded Firebase credentials from file: {}", candidate);
                            break;
                        }
                    }
                }

                // 4. Fallback to classpath
                if (serviceAccount == null) {
                    try {
                        ClassPathResource resource = new ClassPathResource("config/firebase-service-account.json");
                        if (resource.exists()) {
                            serviceAccount = resource.getInputStream();
                            LOG.info("Loaded Firebase credentials from classpath: config/firebase-service-account.json");
                        }
                    } catch (Exception ignored) {}
                }

                if (serviceAccount != null) {
                    FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                    FirebaseApp.initializeApp(options);
                    LOG.info("Firebase Application initialized successfully for project: {}", options.getProjectId());
                } else {
                    LOG.warn("Firebase service account JSON not found. Push notifications will be skipped.");
                }
            }
        } catch (Exception e) {
            LOG.error("Failed to initialize Firebase Admin SDK: {}", e.getMessage());
        }
    }

    @Bean
    public FirebaseMessaging firebaseMessaging() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseMessaging.getInstance();
        }
        return null;
    }
}
