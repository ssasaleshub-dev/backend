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

        // Strip Meta Ads 'p:' or 'P:' or 'tel:' prefix
        if (cleaned.toLowerCase().startsWith("p:")) {
            cleaned = cleaned.substring(2).trim();
        } else if (cleaned.toLowerCase().startsWith("tel:")) {
            cleaned = cleaned.substring(4).trim();
        }

        // Remove whitespace, hyphens, parentheses, and dots
        cleaned = cleaned.replaceAll("[\\s\\-\\(\\)\\.]", "");

        if (cleaned.isEmpty()) {
            return "";
        }

        // Extract pure digits
        String digitsOnly = cleaned.replaceAll("[^0-9]", "");

        // If string already started with '+', preserve international format
        if (cleaned.startsWith("+")) {
            return "+" + digitsOnly;
        }

        // If 10 digits (e.g. 9025099944), prepend +91
        if (digitsOnly.length() == 10) {
            return "+91" + digitsOnly;
        }

        // If 11 digits starting with 0 (e.g. 09025099944), replace leading 0 with +91
        if (digitsOnly.length() == 11 && digitsOnly.startsWith("0")) {
            return "+91" + digitsOnly.substring(1);
        }

        // If 12 digits starting with 91 (e.g. 919025099944), prepend +
        if (digitsOnly.length() == 12 && digitsOnly.startsWith("91")) {
            return "+" + digitsOnly;
        }

        return "+" + digitsOnly;
    }
}
