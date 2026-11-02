package com.salescrm.service.util;

/**
 * SSA Saleshub - Production Lead ID Encryption & Decryption Utility
 * 
 * Provides two-way obfuscation/encryption for database lead sequence IDs.
 * Prevents URL enumeration, scraping, and competitor data harvesting by ensuring
 * raw sequential database IDs (e.g. 959, 960) are NEVER exposed openly in URLs.
 * 
 * Uses a 128-bit Tiny Encryption Algorithm (TEA) with checksum validation and URL-safe Base64 encoding.
 * 100% binary matching with TypeScript leadIdCrypto.ts.
 */
public final class LeadIdCrypto {

    private static final int DELTA = 0x9e3779b9;
    private static final int[] CRYPTO_KEY = { 0x53534153, 0x616c6573, 0x6875624b, 0x65793236 }; // 'SSAS', 'ales', 'hubK', 'ey26'
    private static final int SALT_MAGIC = 0x5a3c96e5;
    private static final String B64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    private LeadIdCrypto() {}

    private static int[] teaEncrypt(int v0, int v1, int[] k) {
        int sum = 0;
        for (int i = 0; i < 32; i++) {
            sum += DELTA;
            v0 += (((v1 << 4) + k[0]) ^ (v1 + sum) ^ ((v1 >>> 5) + k[1]));
            v1 += (((v0 << 4) + k[2]) ^ (v0 + sum) ^ ((v0 >>> 5) + k[3]));
        }
        return new int[] { v0, v1 };
    }

    private static int[] teaDecrypt(int v0, int v1, int[] k) {
        int sum = DELTA * 32;
        for (int i = 0; i < 32; i++) {
            v1 -= (((v0 << 4) + k[2]) ^ (v0 + sum) ^ ((v0 >>> 5) + k[3]));
            v0 -= (((v1 << 4) + k[0]) ^ (v1 + sum) ^ ((v1 >>> 5) + k[1]));
            sum -= DELTA;
        }
        return new int[] { v0, v1 };
    }

    private static String encode64(int w0, int w1) {
        int[] bytes = new int[] {
            (w0 >>> 24) & 0xff, (w0 >>> 16) & 0xff, (w0 >>> 8) & 0xff, w0 & 0xff,
            (w1 >>> 24) & 0xff, (w1 >>> 16) & 0xff, (w1 >>> 8) & 0xff, w1 & 0xff
        };
        StringBuilder res = new StringBuilder(11);
        res.append(B64_CHARS.charAt(bytes[0] >> 2));
        res.append(B64_CHARS.charAt(((bytes[0] & 3) << 4) | (bytes[1] >> 4)));
        res.append(B64_CHARS.charAt(((bytes[1] & 15) << 2) | (bytes[2] >> 6)));
        res.append(B64_CHARS.charAt(bytes[2] & 63));

        res.append(B64_CHARS.charAt(bytes[3] >> 2));
        res.append(B64_CHARS.charAt(((bytes[3] & 3) << 4) | (bytes[4] >> 4)));
        res.append(B64_CHARS.charAt(((bytes[4] & 15) << 2) | (bytes[5] >> 6)));
        res.append(B64_CHARS.charAt(bytes[5] & 63));

        res.append(B64_CHARS.charAt(bytes[6] >> 2));
        res.append(B64_CHARS.charAt(((bytes[6] & 3) << 4) | (bytes[7] >> 4)));
        res.append(B64_CHARS.charAt((bytes[7] & 15) << 2));
        return res.toString();
    }

    private static int[] decode64(String str) {
        if (str == null || str.length() != 11) return null;
        int[] vals = new int[11];
        for (int i = 0; i < str.length(); i++) {
            int idx = B64_CHARS.indexOf(str.charAt(i));
            if (idx == -1) return null;
            vals[i] = idx;
        }
        int b0 = (vals[0] << 2) | (vals[1] >> 4);
        int b1 = ((vals[1] & 15) << 4) | (vals[2] >> 2);
        int b2 = ((vals[2] & 3) << 6) | vals[3];

        int b3 = (vals[4] << 2) | (vals[5] >> 4);
        int b4 = ((vals[5] & 15) << 4) | (vals[6] >> 2);
        int b5 = ((vals[6] & 3) << 6) | vals[7];

        int b6 = (vals[8] << 2) | (vals[9] >> 4);
        int b7 = ((vals[9] & 15) << 4) | (vals[10] >> 2);

        int w0 = (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
        int w1 = (b4 << 24) | (b5 << 16) | (b6 << 8) | b7;
        return new int[] { w0, w1 };
    }

    /**
     * Encrypt a numeric database lead ID into a secure URL-safe token.
     * E.g.: 959L -> "e_NAkEM2fA7YM"
     */
    public static String encrypt(Long id) {
        if (id == null || id <= 0) return "";
        int num = id.intValue();
        int chk = (num ^ SALT_MAGIC) * 31337 + 7919;
        int[] enc = teaEncrypt(num, chk, CRYPTO_KEY);
        return "e_" + encode64(enc[0], enc[1]);
    }

    /**
     * Decrypt an encrypted lead token back to its numeric database ID.
     * Returns null if the token is invalid or tampered with.
     * If the input is already a numeric string (legacy/direct input), returns that number.
     */
    public static Long decrypt(String token) {
        if (token == null || token.isBlank()) return null;
        String str = token.trim();
        // If already numeric ID (e.g. "959"), allow it for backwards compatibility
        if (str.matches("^\\d+$")) {
            return Long.parseLong(str);
        }
        if (!str.startsWith("e_")) return null;
        String raw = str.substring(2);
        int[] decoded = decode64(raw);
        if (decoded == null) return null;
        int[] dec = teaDecrypt(decoded[0], decoded[1], CRYPTO_KEY);
        int expectedChk = (dec[0] ^ SALT_MAGIC) * 31337 + 7919;
        if (dec[1] != expectedChk) return null;
        return (long) dec[0];
    }
}
