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
        "ewogICJ0eXBlIjogInNlcnZpY2VfYWNjb3VudCIsCiAgInByb2plY3RfaWQiOiAic3NhLXNhbGVzLWh1" +
        "YiIsCiAgInByaXZhdGVfa2V5X2lkIjogImVhMGNmNjgzNTE3ZjhiZTNhZWJjNjc3NmEyZDJhM2ZhNjI2" +
        "ZjJhYmUiLAogICJwcml2YXRlX2tleSI6ICItLS0tLUJFR0lOIFBSSVZBVEUgS0VZLS0tLS1cbk1JSUV2" +
        "QUlCQURBTkJna3Foa2lHOXcwQkFRRUZBQVNDQktZd2dnU2lBZ0VBQW9JQkFRQ2tGUzNKaEpFUC82ZTJc" +
        "bjMwSCs2TWNFSzhaSXNLdlVvRmNBNXR3Q2RZcTRWV2J2Q1NybS9nRWdKei9Wb09wWmVhYkJhcjc5c3No" +
        "Smp4cFlcbmZpSUpHWmpzS2dOVWlNb0phMXRjemROWk9FbGNtbDdrTHhOSmN4dFhtZzhFbEdFZHJWNXlV" +
        "N0dUUUtQMk95ZlRcbll1dTRVV1FrRGRtTzdGYlluTlBwSkh1Z240bWgwM0tqWjhIa0pRcWkvbmRBdmU3" +
        "TkFCNjRiWDIya2Z2Zy85aGVcbjQvdVpMZVJjVDBjbklFWmNCTlNtak9RKzQ2WStWTUtRV25JV3RGaElk" +
        "N295OW5XMUJUMnV3MlRWOEJmQ3FFbDBcbm9ZQk43U293dktFOXBPMVRqQ3RJb2VXb1lvdkNjYU1obmdr" +
        "TmpCOTVQNjFVTFdZMG5BdGJjNjZHN2FpU0VZVGhcbnVqalJML3BSQWdNQkFBRUNnZ0VBUzRCMVY3ZTQr" +
        "bmdrNStpMm1RSHE1ZmZ3bnkwd2ZFQWxtTVFpTTcxN2poTzZcbk1EN3pMMlJ0QnlzWVhBaFdIY0wxSVNZ" +
        "cDZWcDFnQ2VwczFPaS9MWDdJSnk2ZUpFVENwK01aRmxQcWN2Vk1oZlVcbmwwSTBLVVdlck1JMnErQk5D" +
        "ZGRuVU10RE9HTnpydytNWldBczRGMFBhK20xbE9jYWxVVWcyN1NPY0p4SXZCM3dcbkMvRGdTSWhvSExH" +
        "VnZOQ1M2VXQwRys3ZlNvSEZKdGpnT2E1cW1MU2NabjRWR29iOUU4SVNPTXVQMCt4ektYWEZcbm9JQXls" +
        "Y3h3bGVMNTFiSHJHYTU0OGZCZFI0QUdXQ0gxNlFkU1M0YTRQbXNwNW83UnVKclJDNkpGMXRPbnNlWm9c" +
        "bnl5dVlmamZHUkJxZTN2LzZtNW1DeEZ5ejJGR3VFRTh6aDlvc3NxNllBd0tCZ1FEUUk2WVVvWElhYzl0" +
        "VmFsN2pcbm1HNkc1S09QTVNnY05uOUxkTHR1QnJCRkdwVDVNMUV1SUg0ckx5S3R0aCtBVkpGcjFQOGkx" +
        "WlNqbzJMRXVaRHNcbnBMY1VKdFNJT2pZT0JPMnkrK2RHRVlHdm9TRTQzZElHdlNJMHpZdHVHMEwzVWNV" +
        "UTlNdTNjeFVwMkNJVC9IYXJcbkdWYk04aFpvTFFBRnhpNy9GZmRRYzFZeEh3S0JnUURKMEJmandidFMv" +
        "TURJYXpsZUVXemFjUnVtVDJGWVdHaVhcbi95UmpBLzNsSEx5algxdzFEUDMyaHVXQnhkYWJWOGt5REdI" +
        "RHlKbXh1dmRUYzJXZlBaQm10VVFsSnhKbHk0ZUtcbjhxTC9xc2dXZGc5ZnQ4UzlGdWg0TnhJZ0JSUHlC" +
        "dldIU3B4T1NTWnFZRU8xOW1KRnZaSlVhWHF0MnBSZjQ5d2pcbjVsZlRqcEkyandLQmdHS3JRNlFzRk1k" +
        "dGZPQzFHRjZVZUhFQ0duZGJPa1NGb1RySUQ4Sk04V3NRK3VaWDZYZ1FcbmR6OUpqUXNFZDFLQ3hKMTF6" +
        "NjFYeXFyNFhXNGMraEwwMHNWKzB1bnhqb1orMTZBdHdxbDR5eDNUS01pb2YxUXFcbmZQakRLQUgrY25T" +
        "RUV3UEN5SUFpMGptUCtEZTVTeGc1WHVpeEt1WWZ6dzd0c0lIRWpBVmJaOVRKQW9HQUlONkZcbnBaQytJ" +
        "OHdLMGhtMHBEK09UUjJ5OVNsa24zRDJ5QnJFSUdSY0M5N2NVbmVMbXhCY3BMUmYyUDBSOFFjaE84ZU1c" +
        "bmZNakZUNDgyRFI5MFk3VXhPYWs0U2h6TkFFdGY3aCtLc0wxdURVcDVRb3U1dlZwbnpueVp0U0ZlbUVM" +
        "TjVwVWVcblZCblI2ZTEzMmNFaFRGNk40TFcxUjIrY1R1bmJ4dlVUUk4rcHJMMENnWUE4bnJrdS9jWitl" +
        "TjYvYzYwMFE4VWtcbjlSL2taYUVqcDlqSTJwcjV1dnBOUi9vMjJ1aFl0Uk9ZYWsrUEZGZ005RmxtcTc4" +
        "MVpJKyszd1BmT3lLRVBNR01cbnN6TGVtN2tJaU1ESDI5aWwvaG9pYkNHYW5HZkp2K2J5eU12RDk2VmVI" +
        "OGtwUGRxR3FyaTJQcjk1eVZUaDJ5WWJcbjlobGpvUXMzakh3SEhoenJtRzRPb0E9PVxuLS0tLS1FTkQg" +
        "UFJJVkFURSBLRVktLS0tLVxuIiwKICAiY2xpZW50X2VtYWlsIjogImZpcmViYXNlLWFkbWluc2RrLWZi" +
        "c3ZjQHNzYS1zYWxlcy1odWIuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLAogICJjbGllbnRfaWQiOiAi" +
        "MTAzNTQwNTQ0NDQ5NzAzMTUwNzMwIiwKICAiYXV0aF91cmkiOiAiaHR0cHM6Ly9hY2NvdW50cy5nb29n" +
        "bGUuY29tL28vb2F1dGgyL2F1dGgiLAogICJ0b2tlbl91cmkiOiAiaHR0cHM6Ly9vYXV0aDIuZ29vZ2xl" +
        "YXBpcy5jb20vdG9rZW4iLAogICJhdXRoX3Byb3ZpZGVyX3g1MDlfY2VydF91cmwiOiAiaHR0cHM6Ly93" +
        "d3cuZ29vZ2xlYXBpcy5jb20vb2F1dGgyL3YxL2NlcnRzIiwKICAiY2xpZW50X3g1MDlfY2VydF91cmwi" +
        "OiAiaHR0cHM6Ly93d3cuZ29vZ2xlYXBpcy5jb20vcm9ib3QvdjEvbWV0YWRhdGEveDUwOS9maXJlYmFz" +
        "ZS1hZG1pbnNkay1mYnN2YyU0MHNzYS1zYWxlcy1odWIuaWFtLmdzZXJ2aWNlYWNjb3VudC5jb20iLAog" +
        "ICJ1bml2ZXJzZV9kb21haW4iOiAiZ29vZ2xlYXBpcy5jb20iCn0K";

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
