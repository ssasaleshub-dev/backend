package com.salescrm.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateTimeParserTest {

    private DateTimeParser dateTimeParser;

    @BeforeEach
    void setUp() {
        dateTimeParser = new DateTimeParser();
    }

    @Test
    void shouldParseIsoOffsetDateTime() {
        String input = "2026-05-28T04:22:59-05:00";
        Instant parsed = dateTimeParser.parse(input);
        assertThat(parsed).isNotNull();
        assertThat(parsed.toString()).isEqualTo("2026-05-28T09:22:59Z");
    }

    @Test
    void shouldParseIsoInstant() {
        String input = "2026-05-28T09:22:59Z";
        Instant parsed = dateTimeParser.parse(input);
        assertThat(parsed).isNotNull();
        assertThat(parsed.toString()).isEqualTo("2026-05-28T09:22:59Z");
    }

    @Test
    void shouldParseStandardDateTimeWithSpace() {
        String input = "2026-05-28 04:22:59";
        Instant parsed = dateTimeParser.parse(input);
        assertThat(parsed).isNotNull();
        assertThat(parsed.toString()).isEqualTo("2026-05-28T04:22:59Z");
    }

    @Test
    void shouldParseSlashSeparatedDateTime() {
        String input = "28/05/2026 04:22:59";
        Instant parsed = dateTimeParser.parse(input);
        assertThat(parsed).isNotNull();
        assertThat(parsed.toString()).isEqualTo("2026-05-28T04:22:59Z");
    }

    @Test
    void shouldParseDateOnly() {
        String input = "2026-05-28";
        Instant parsed = dateTimeParser.parse(input);
        assertThat(parsed).isNotNull();
        assertThat(parsed.toString()).isEqualTo("2026-05-28T00:00:00Z");
    }

    @Test
    void shouldParseTwoDigitYearFormat() {
        String input = "10-18-24 12:52:17";
        Instant parsed = dateTimeParser.parse(input);
        assertThat(parsed).isNotNull();
        assertThat(parsed.toString()).isEqualTo("2024-10-18T12:52:17Z");
    }

    @Test
    void shouldFallbackGracefullyOnNullOrBlank() {
        Instant now = Instant.now();
        Instant parsedNull = dateTimeParser.parse(null);
        Instant parsedBlank = dateTimeParser.parse("   ");

        assertThat(parsedNull).isNotNull();
        assertThat(parsedBlank).isNotNull();
        assertThat(parsedNull).isAfterOrEqualTo(now.minusSeconds(2));
        assertThat(parsedBlank).isAfterOrEqualTo(now.minusSeconds(2));
    }
}
