package com.salescrm.service.util;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Verifies fingerprint-gated attendance signatures.
 *
 * The phone signs {@link #signedMessage} with an EC P-256 key held in Android Keystore that can
 * only be used after a successful fingerprint scan (SHA256withECDSA, DER-encoded signature).
 * The server verifies with the public key stored at registration.
 */
public final class AttendanceSignatureVerifier {

    private AttendanceSignatureVerifier() {}

    /** The exact text the phone signs. Must match the Android app byte for byte. */
    public static String signedMessage(Long challengeId, String nonce, String userId) {
        return "SSA-ATTENDANCE|v1|" + challengeId + "|" + nonce + "|" + userId.trim().toLowerCase();
    }

    /** Parses a Base64 X.509 EC public key; throws IllegalArgumentException if it is not one. */
    public static PublicKey parsePublicKey(String base64) {
        if (base64 == null || base64.isBlank()) {
            throw new IllegalArgumentException("Public key is missing");
        }
        try {
            byte[] der = Base64.getDecoder().decode(base64.trim());
            PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(der));
            if (!(key instanceof ECPublicKey ec) || ec.getParams().getCurve().getField().getFieldSize() != 256) {
                throw new IllegalArgumentException("Public key must be an EC P-256 key");
            }
            return key;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid public key", e);
        }
    }

    /** True only when {@code signatureBase64} is a valid signature of {@code message} by the key. */
    public static boolean verify(String publicKeyBase64, String message, String signatureBase64) {
        if (signatureBase64 == null || signatureBase64.isBlank()) {
            return false;
        }
        try {
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(parsePublicKey(publicKeyBase64));
            verifier.update(message.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(signatureBase64.trim()));
        } catch (Exception e) {
            return false;
        }
    }
}
