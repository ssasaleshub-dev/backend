package com.salescrm.service.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.service.dto.facebook.FacebookFieldDataDTO;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Helpers for keeping every answer a candidate gave on a Meta Instant Form (or a Meta lead export),
 * not just the handful of fields the CRM maps to its own columns.
 */
public final class MetaLeadDataUtil {

    private static final ObjectMapper JSON = new ObjectMapper();

    private MetaLeadDataUtil() {}

    /** "what_is_your_current_status?" → "What is your current status?" */
    public static String humanizeQuestion(String key) {
        if (key == null || key.isBlank()) return "";
        String text = key.trim().replace('_', ' ').replaceAll("\\s+", " ").trim();
        if (text.isEmpty()) return "";
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** Readable answer: "completed_ug" → "Completed ug" stays close to the original but drops underscores. */
    public static String humanizeAnswer(String value) {
        if (value == null) return "";
        return value.trim().replace('_', ' ').replaceAll("\\s+", " ");
    }

    /** All Graph API field_data answers as JSON [{"key","label","value"}], in form order. */
    public static String toFormResponsesJson(List<FacebookFieldDataDTO> fieldData) {
        if (fieldData == null || fieldData.isEmpty()) return null;
        List<Map<String, String>> out = new ArrayList<>();
        for (FacebookFieldDataDTO f : fieldData) {
            if (f == null || f.getName() == null || f.getName().isBlank()) continue;
            List<String> values = f.getValues() != null ? f.getValues() : List.of();
            String value = String.join(", ", values.stream().filter(v -> v != null && !v.isBlank()).toList());
            out.add(entry(f.getName(), value));
        }
        return write(out);
    }

    /**
     * All columns of an imported Meta lead export row as JSON [{"key","label","value"}].
     * {@code row} keeps the spreadsheet's column order (header → cell value).
     */
    public static String toFormResponsesJson(Map<String, String> row) {
        if (row == null || row.isEmpty()) return null;
        List<Map<String, String>> out = new ArrayList<>();
        row.forEach((key, value) -> {
            if (key == null || key.isBlank()) return;
            if (value == null || value.isBlank()) return;
            out.add(entry(key, value.trim()));
        });
        return write(out);
    }

    private static final java.util.Set<String> ATTRIBUTION_COLUMNS = java.util.Set.of(
        "id", "lead_id", "created_time", "created_at", "time", "date",
        "ad_id", "ad id", "ad_name", "ad name",
        "adset_id", "ad_set_id", "adset id", "ad set id", "adset_name", "ad_set_name", "adset name", "ad set name",
        "campaign_id", "campaign id", "campaign_name", "campaign name",
        "form_id", "form id", "form_name", "form name",
        "is_organic", "is organic", "organic", "platform", "publisher_platform", "lead_status", "status"
    );

    /**
     * True for Meta export columns that describe the ad/attribution (stored in their own fields),
     * false for the candidate's actual form answers.
     */
    public static boolean isAttributionColumn(String header) {
        return header != null && ATTRIBUTION_COLUMNS.contains(header.trim().toLowerCase());
    }

    /**
     * Builds the candidate's form answers from an import row, in spreadsheet column order.
     *
     * @param headerIndex header name → column index
     * @param valueAt     column index → cell text
     */
    public static String formResponsesFromRow(Map<String, Integer> headerIndex, java.util.function.IntFunction<String> valueAt) {
        if (headerIndex == null || headerIndex.isEmpty()) return null;
        Map<String, String> ordered = new LinkedHashMap<>();
        headerIndex
            .entrySet()
            .stream()
            .sorted(Map.Entry.comparingByValue())
            .forEach(e -> {
                if (isAttributionColumn(e.getKey())) return;
                String v;
                try {
                    v = valueAt.apply(e.getValue());
                } catch (Exception ex) {
                    v = null;
                }
                if (v != null && !v.isBlank()) ordered.put(e.getKey(), v);
            });
        return toFormResponsesJson(ordered);
    }

    /** Maps Meta "current status" style answers to the CRM's qualification labels. */
    public static String mapEducationalQualification(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String norm = raw.trim().toLowerCase().replace("-", " ").replace("_", " ");
        if (norm.contains("completed ug") || norm.contains("bachelor") || norm.contains("graduate")) {
            return "Bachelor Degree / UG Graduate";
        }
        if (norm.contains("final year") || norm.contains("finallyear")) {
            return "Final Year UG Student";
        }
        if (norm.contains("other")) {
            return "Other";
        }
        return raw.trim();
    }

    private static Map<String, String> entry(String key, String value) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("key", key.trim());
        m.put("label", humanizeQuestion(key));
        m.put("value", value == null ? "" : value);
        return m;
    }

    private static String write(List<Map<String, String>> list) {
        if (list.isEmpty()) return null;
        try {
            return JSON.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }
}
