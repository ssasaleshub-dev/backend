package com.salescrm.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.config.FacebookProperties;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacebookWebhookVerificationServiceTest {

    private static final String APP_SECRET = "test_meta_app_secret_123456";
    private static final String VERIFY_TOKEN = "test_verify_token_xyz";

    private FacebookProperties properties;
    private FacebookWebhookVerificationService service;

    @BeforeEach
    void setUp() {
        properties = new FacebookProperties();
        properties.setAppSecret(APP_SECRET);
        properties.setVerifyToken(VERIFY_TOKEN);
        service = new FacebookWebhookVerificationService(properties);
    }

    @Test
    void shouldSuccessfullyVerifySubscriptionToken() {
        boolean result = service.verifySubscriptionToken("subscribe", VERIFY_TOKEN);
        assertThat(result).isTrue();
    }

    @Test
    void shouldRejectWhenModeIsNotSubscribe() {
        boolean result = service.verifySubscriptionToken("unsubscribe", VERIFY_TOKEN);
        assertThat(result).isFalse();
    }

    @Test
    void shouldRejectWhenTokenDoesNotMatch() {
        boolean result = service.verifySubscriptionToken("subscribe", "wrong_token");
        assertThat(result).isFalse();
    }

    @Test
    void shouldRejectWhenTokenIsNull() {
        boolean result = service.verifySubscriptionToken("subscribe", null);
        assertThat(result).isFalse();
    }

    @Test
    void shouldSuccessfullyVerifyValidHmacSha256Signature() throws Exception {
        String payload = "{\"object\":\"page\",\"entry\":[]}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(APP_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String hash = HexFormat.of().formatHex(mac.doFinal(payloadBytes));
        String signatureHeader = "sha256=" + hash;

        boolean isValid = service.verifySignature(signatureHeader, payloadBytes);
        assertThat(isValid).isTrue();
    }

    @Test
    void shouldRejectInvalidHmacSignature() {
        String payload = "{\"object\":\"page\",\"entry\":[]}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        boolean isValid = service.verifySignature("sha256=invalidhash1234567890abcdef", payloadBytes);
        assertThat(isValid).isFalse();
    }

    @Test
    void shouldRejectMissingOrMalformedSignatureHeader() {
        String payload = "{\"object\":\"page\"}";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);

        assertThat(service.verifySignature(null, payloadBytes)).isFalse();
        assertThat(service.verifySignature("malformed_signature_without_prefix", payloadBytes)).isFalse();
    }
}
