package com.salescrm.service.util;

import org.springframework.stereotype.Component;

/**
 * Single Responsibility: Phone number normalization and sanitization.
 * Strips prefix indicators (such as 'p:' commonly found in Meta Lead Ads export)
 * and guarantees a clean, consistent format for database indexing and deduplication.
 */
@Component
public class PhoneNormalizer {

    /**
     * Normalizes a raw phone number.
     * E.g., "p:+917010202931" -> "+917010202931"
     * "  +91 7010 202931  " -> "+917010202931"
     *
     * @param rawPhone raw phone string from input
     * @return normalized phone number
     */
    public String normalize(String rawPhone) {
        if (rawPhone == null) {
            return null;
        }

        String cleaned = rawPhone.trim();

        // Strip Meta Ads 'p:' or 'P:' prefix
        if (cleaned.toLowerCase().startsWith("p:")) {
            cleaned = cleaned.substring(2).trim();
        }

        // Remove whitespace and hyphens
        cleaned = cleaned.replaceAll("[\\s\\-\\(\\)]", "");

        return cleaned;
    }
}
