package com.salescrm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.InputStream;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class FirebaseConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(FirebaseConfiguration.class);

    private static final String EMBEDDED_FIREBASE_CREDENTIALS =
        "ewogICJ0eXBlIjogInNlcnZpY2VfYWNjb3VudCIsCiAgInByb2plY3RfaWQiOiAic3NhLXNhbGVzaHViIiwK" +
        "ICAicHJpdmF0ZV9rZXlfaWQiOiAiOThiN2QyZDJhY2FmYWJiNTgwNGNiMzVmMTNiYjdmZjA2ODY5ZTU4NCIs" +
        "CiAgInByaXZhdGVfa2V5IjogIi0tLS0tQkVHSU4gUFJJVkFURSBLRVktLS0tLVxuTUlJRXZnSUJBREFOQmdr" +
        "cWhraUc5dzBCQVFFRkFBU0NCS2d3Z2dTa0FnRUFBb0lCQVFDd0RONnIzYWJPZ1BQNFxuU1VSZThvVmxaMzBW" +
        "cDE4d1FzSEpOR0w3UDlZc0xCR2dtR2RIU1A3dm01OEY5MFBQZkR5K3ovd0NqcFVNUEFPZVxuQjRNb2JKc2tX" +
        "a0UwRDhlZlhRUmt3UVFSU0xONlBiZk01QjJ5blgxNnBzVm1sbWVpWXJMNjVaejcyRHlETDltbVxuNW5kaEdI" +
        "R3J3SXdQT3Ewd2d4Q2JrbWtyTzhpZ2g2R1RDa2RYK3AvVmlpazd2Z0cxT0QrUGltdndlSitDUUxQaFxuU2hS" +
        "TW8xbEFrMm1yanNOT1VXQmI1V1JZZWlETlNDamVvK1VDTU03TWdHVStid2Z1aDNMOUhnem9EbG5HbE9Palxu" +
        "MWdZSTJmeGF2cm5hakFiTWMvS2pZVm9tQUhLV2d3R3N4NXY2WVA0R29BQStGSEszZ3ZIRXhhemJobmZNZHpk" +
        "SlxubFlYMksyQ1ZBZ01CQUFFQ2dnRUFDQkl3d1ByTk9OSXlNb0FvZzBlZWFjUzQ4UUZQZVJpYnU4VHZuK0FR" +
        "V0pUNFxub2x0c0hFUDVyUHNvNnh6OVFXMVpkNWRZVU8vcWZFZHdGRm1qbUxPZlNvck9nNUVRdnBkTUcrOUlo" +
        "Qjg2SXpmK1xuc1V3aHhqSEYrVUJPOUVXZjJaTEY3TGt1enljekRGZE1zeEkrZVV6N0NEdjNmaEhwbHZURjla" +
        "YVZpL1VXZVhndVxuWmNZWmNmQkMwMkZKSEh2ZXJ2ditzZTBQd0JrWlRFWkhBNlJIT1VKNW1vaWJCSCtmZm1x" +
        "Skp5d291MG9XNkdsZ1xuV3dTS3lQMXhGaVY0cDBnK3JzNlA3eERqbE0vWGpaNGhJWFR5cyt4QmtreEhOTDl4" +
        "QXJ6K3dEL0JaVy9mTTRrUVxuN3Nkcm9WRkxqajA5bUlNUWN5UnVxY1ljbk52aC9LeXRLMHJNVjdUaHJRS0Jn" +
        "UURtT1dtZDdDQnJ0Tm1EanBRSVxuNGp5N1ZOMnFrRnloMWlqMVFqa0JhdjZlLzk3VXA4TXg2K3VyMTMyUmFI" +
        "OXZKRExKaDEzcUFKMHdRK000UDUrR1xuZFRNNmpEaTNlZ1ZOWmcvbmZUUlhReFl3ejl4bW9pdUFXUmFLb3V0" +
        "ZWxSMjg2OS9OM2pRdG1pWXFWaGtzL05LNFxuRlBrY0dCOXd6UzdhbDNQZXZlYm5FMnNKWXdLQmdRRER3cjd3" +
        "R1hkYjFiMHJETG45Z1B5a1c1cHRIVE1BQnJEMlxueSt6aURUVlcrdDRmeGxpdmE0OFZyeDBOTGIrYU9YMUxN" +
        "djZpWGg0QXZoekZnektoRzgwdlVXZUc2Vll6Mm9MZ1xuVlh5dVhmRk1tU1VYYkh4U2Y3REhmRE03WUhVYUZs" +
        "bXluMThJb3VUZ1c3elpYM0ErZzFYZUMzR1h5M1pXcUhiMlxuUjUzTTRac0xwd0tCZ0gvYUJHQlNJRk1RRlBk" +
        "dE42S1RXUnpEYlhKcVJ6Y013cVFwU1pVVEFTSDFnSjdpSjVGU1xucmdjNVhaOWh3OXRKNC9acytXZVpQYkh2" +
        "RVlGTU8rQWt0L1ZjcDZhazJjcjQwZjVCVnM3YkQ3aTl4bTBzeERHN1xueDd2bjdBUVRsWGMwR282dVJwWEls" +
        "RWRRbHh5WHJzSFJ5K0doTjl5aDVkSTQ1MzBjUXQyeXQrOTNBb0dCQUtHL1xuV1pCOUdqZ1JCTHA3UzNvQWg3" +
        "OHR5UXo3SVZhV01NWGFmU0hTRkE2SVBkdzNRTFBBeFhyL001dmQvd0JwRy9OMFxuY1J5cmMxTGR3bHdZS1U4" +
        "Zzc0cDNVbmtRZHg5bEh0cEpDRG55ZVJ2M05jU0M2TDRwcjJGRVovaFpuS2d2YTNIQlxuYVA1enlYVHZsN3hy" +
        "dFF5UjYvTUp0WlBja3l6TENYQTRnRlAyM09ybEFvR0JBTWJWU2hld2IxQjVWUXBNOEsrRlxuMWthbTNsRlpx" +
        "ZGluekpxUUx2VmNZRGd0bG43NFRuOXNuRU1jWmtoY3FUYk5hRW9TNCtyaUFXaWd5WkNNSC9BeVxub3AvTzcw" +
        "ZTBTZnVOZHl0aVVnZmJHUnowVjl3Q25zZ0FrQ29uRGtraW1YTU1uUnNlTXNOVUFPd1R5dE8vYWlYUVxuamU3" +
        "SmZ4aTl4cVpEczErdXU3WmpjbWxEXG4tLS0tLUVORCBQUklWQVRFIEtFWS0tLS0tXG4iLAogICJjbGllbnRf" +
        "ZW1haWwiOiAiZmlyZWJhc2UtYWRtaW5zZGstZmJzdmNAc3NhLXNhbGVzaHViLmlhbS5nc2VydmljZWFjY291" +
        "bnQuY29tIiwKICAiY2xpZW50X2lkIjogIjEwNzQxNzY4MDk1OTk2OTc4NTgzNiIsCiAgImF1dGhfdXJpIjog" +
        "Imh0dHBzOi8vYWNjb3VudHMuZ29vZ2xlLmNvbS9vL29hdXRoMi9hdXRoIiwKICAidG9rZW5fdXJpIjogImh0" +
        "dHBzOi8vb2F1dGgyLmdvb2dsZWFwaXMuY29tL3Rva2VuIiwKICAiYXV0aF9wcm92aWRlcl94NTA5X2NlcnRf" +
        "dXJsIjogImh0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL29hdXRoMi92MS9jZXJ0cyIsCiAgImNsaWVudF94" +
        "NTA5X2NlcnRfdXJsIjogImh0dHBzOi8vd3d3Lmdvb2dsZWFwaXMuY29tL3JvYm90L3YxL21ldGFkYXRhL3g1" +
        "MDkvZmlyZWJhc2UtYWRtaW5zZGstZmJzdmMlNDBzc2Etc2FsZXNodWIuaWFtLmdzZXJ2aWNlYWNjb3VudC5j" +
        "b20iLAogICJ1bml2ZXJzZV9kb21haW4iOiAiZ29vZ2xlYXBpcy5jb20iCn0K";

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
                        Path p = Path.of(envPath);
                        if (Files.exists(p)) {
                            serviceAccount = Files.newInputStream(p);
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
                    for (String path : candidates) {
                        Path p = Path.of(path);
                        if (Files.exists(p)) {
                            serviceAccount = Files.newInputStream(p);
                            LOG.info("Loaded Firebase credentials from file: {}", path);
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

                // 5. Built-in resilient embedded fallback for ssa-saleshub project
                if (serviceAccount == null) {
                    try {
                        byte[] decoded = Base64.getDecoder().decode(EMBEDDED_FIREBASE_CREDENTIALS.trim());
                        serviceAccount = new ByteArrayInputStream(decoded);
                        LOG.info("Loaded Firebase credentials from embedded application fallback for project ssa-saleshub");
                    } catch (Exception e) {
                        LOG.warn("Failed to decode embedded Firebase credentials: {}", e.getMessage());
                    }
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
