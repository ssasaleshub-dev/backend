package com.salescrm.service;

import com.salescrm.config.FacebookProperties;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for verifying Meta/Facebook Webhooks.
 *
 * Implements:
 * 1. GET hub.challenge verification token checking.
 * 2. POST X-Hub-Signature-256 HMAC-SHA256 signature verification.
 */
@Service
public class FacebookWebhookVerificationService {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookWebhookVerificationService.class);
    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGNATURE_PREFIX = "sha256=";

    private final FacebookProperties facebookProperties;

    public FacebookWebhookVerificationService(FacebookProperties facebookProperties) {
        this.facebookProperties = facebookProperties;
    }

    /**
     * Validates the verification token sent by Meta during webhook subscription setup.
     *
     * @param mode hub.mode from Meta (must be "subscribe")
     * @param token hub.verify_token from Meta
     * @return true if valid, false otherwise
     */
    public boolean verifySubscriptionToken(String mode, String token) {
        if (!"subscribe".equalsIgnoreCase(mode)) {
            LOG.warn("Meta webhook verification rejected: mode is '{}', expected 'subscribe'", mode);
            return false;
        }

        byte[] tokenBytes = (token != null ? token : "").getBytes(StandardCharsets.UTF_8);

        String configuredToken = facebookProperties.getVerifyToken();
        if (configuredToken != null && !configuredToken.isBlank() &&
            MessageDigest.isEqual(configuredToken.trim().getBytes(StandardCharsets.UTF_8), tokenBytes)) {
            return true;
        }

        if (facebookProperties.getAccounts() != null) {
            for (FacebookProperties.MetaAccountConfig account : facebookProperties.getAccounts()) {
                if (account != null && account.getVerifyToken() != null && !account.getVerifyToken().isBlank() &&
                    MessageDigest.isEqual(account.getVerifyToken().trim().getBytes(StandardCharsets.UTF_8), tokenBytes)) {
                    LOG.info("Meta webhook verification matched configured account '{}'", account.getName());
                    return true;
                }
            }
        }

        LOG.warn("Meta webhook verification rejected: verify token mismatch or not configured");
        return false;
    }

    /**
     * Verifies the cryptographic HMAC SHA-256 signature in the X-Hub-Signature-256 header.
     * Uses constant-time MessageDigest.isEqual to eliminate timing side-channel attacks.
     *
     * @param signatureHeader value of X-Hub-Signature-256 header (format: "sha256=<hash>")
     * @param payloadBytes raw request body bytes
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(String signatureHeader, byte[] payloadBytes) {
        if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) {
            LOG.warn("Meta signature verification failed: invalid or missing X-Hub-Signature-256 header");
            return false;
        }

        String expectedHash = signatureHeader.substring(SIGNATURE_PREFIX.length()).trim();
        byte[] safePayload = payloadBytes != null ? payloadBytes : new byte[0];

        String primarySecret = facebookProperties.getAppSecret();
        if (primarySecret != null && !primarySecret.isBlank() && matchesHmac(primarySecret.trim(), safePayload, expectedHash)) {
            return true;
        }

        if (facebookProperties.getAccounts() != null) {
            for (FacebookProperties.MetaAccountConfig account : facebookProperties.getAccounts()) {
                if (account != null && account.getAppSecret() != null && !account.getAppSecret().isBlank() &&
                    matchesHmac(account.getAppSecret().trim(), safePayload, expectedHash)) {
                    LOG.info("Meta webhook signature matched configured account '{}'", account.getName());
                    return true;
                }
            }
        }

        LOG.warn("Meta signature verification failed: no configured App Secret matched X-Hub-Signature-256");
        return false;
    }

    private boolean matchesHmac(String secret, byte[] payloadBytes, String expectedHash) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);

            byte[] hmacBytes = mac.doFinal(payloadBytes);
            String calculatedHash = HexFormat.of().formatHex(hmacBytes);

            return MessageDigest.isEqual(
                calculatedHash.toLowerCase().getBytes(StandardCharsets.UTF_8),
                expectedHash.toLowerCase().getBytes(StandardCharsets.UTF_8)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOG.error("HMAC SHA-256 calculation error during Meta webhook signature verification: {}", e.getMessage(), e);
            return false;
        }
    }
}
