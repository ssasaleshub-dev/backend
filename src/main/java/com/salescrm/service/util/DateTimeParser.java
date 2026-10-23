package com.salescrm.service.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Single Responsibility: Date and timestamp parsing utility.
 * Robustly parses various datetime formats from incoming Meta Ads Excel/CSV exports into Java Instant.
 */
@Component
public class DateTimeParser {

    private static final Logger LOG = LoggerFactory.getLogger(DateTimeParser.class);

    private static final DateTimeFormatter[] FORMATTERS = new DateTimeFormatter[] {
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ISO_INSTANT,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("MM-dd-yy HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM-dd-yy HH:mm"),
        DateTimeFormatter.ofPattern("MM/dd/yy HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM/dd/yy HH:mm"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm"),
        DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("MM-dd-yy"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy"),
        DateTimeFormatter.ofPattern("MM/dd/yyyy")
    };

    /**
     * Parses an ISO-8601 or common datetime string into an Instant.
     * E.g. "2026-10-05T02:24:13-05:00", "2026-05-28 04:22:59" -> Instant
     *
     * @param dateString raw date string
     * @return Instant or current timestamp if parsing fails
     */
    public Instant parse(String dateString) {
        if (dateString == null || dateString.isBlank()) {
            return Instant.now();
        }

        String trimmed = dateString.trim();

        // 1. Direct parse if standard ISO offset (e.g. 2026-10-05T02:24:13-05:00)
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException ignored) {
            // continue fallback
        }

        // 2. Direct parse if standard ISO instant (e.g. 2026-10-05T02:24:13Z)
        try {
            return Instant.parse(trimmed);
        } catch (DateTimeParseException ignored) {
            // continue fallback
        }

        // 3. Fallback through common formats
        for (DateTimeFormatter formatter : FORMATTERS) {
            try {
                return OffsetDateTime.parse(trimmed, formatter).toInstant();
            } catch (Exception ignored) {
                // next attempt
            }

            try {
                LocalDateTime ldt = LocalDateTime.parse(trimmed, formatter);
                return ldt.toInstant(ZoneOffset.UTC);
            } catch (Exception ignored) {
                // next attempt
            }

            try {
                LocalDate ld = LocalDate.parse(trimmed, formatter);
                return ld.atStartOfDay().toInstant(ZoneOffset.UTC);
            } catch (Exception ignored) {
                // next attempt
            }
        }

        LOG.warn("Failed to parse date string '{}', falling back to current Instant.", trimmed);
        return Instant.now();
    }
}

