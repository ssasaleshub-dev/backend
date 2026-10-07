package com.salescrm.service.util;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Single Responsibility: Date and timestamp parsing utility.
 * Robustly parses various datetime formats from incoming Excel/CSV exports into Java Instant.
 */
@Component
public class DateTimeParser {

    private static final Logger LOG = LoggerFactory.getLogger(DateTimeParser.class);

    /**
     * Parses an ISO-8601 or common datetime string into an Instant.
     * E.g. "2026-10-05T02:24:13-05:00" -> Instant
     *
     * @param dateString raw date string
     * @return Instant or current timestamp if parsing fails
     */
    public Instant parse(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return Instant.now();
        }

        String trimmed = dateString.trim();

        // Try direct ISO offset datetime (e.g. 2026-10-05T02:24:13-05:00)
        try {
            return OffsetDateTime.parse(trimmed, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant();
        } catch (DateTimeParseException ignored) {
            // continue fallback
        }

        // Try standard Instant parse (e.g. 2026-10-05T02:24:13Z)
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // continue fallback
        }

        // Try standard local date-time without offset
        try {
            return OffsetDateTime.parse(trimmed + "Z").toInstant();
        } catch (Exception e) {
            LOG.warn("Failed to parse date string '{}', falling back to current Instant. Error: {}", trimmed, e.getMessage());
            return Instant.now();
        }
    }
}
