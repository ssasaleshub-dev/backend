package com.salescrm.service.ingestion;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for parsing CSV and TSV lead exports.
 * Supports comma and tab delimiters with RFC 4180 quote escaping.
 */
@Component
public class MetaAdsCsvIngestionStrategy implements LeadIngestionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(MetaAdsCsvIngestionStrategy.class);

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".txt");
    }

    @Override
    public List<ExcelLeadRowDTO> parse(InputStream inputStream) throws Exception {
        List<ExcelLeadRowDTO> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String headerLine = reader.readLine();
        if (headerLine == null || headerLine.isBlank()) {
            return rows;
        }

        // Detect delimiter: tab or comma
        char delimiter = headerLine.contains("\t") ? '\t' : ',';
        List<String> headers = parseLine(headerLine, delimiter);

        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            headerMap.put(normalizeHeader(headers.get(i)), i);
        }

        LOG.debug("Detected CSV/TSV headers (delimiter='{}'): {}", delimiter == '\t' ? "\\t" : ",", headerMap.keySet());

        String line;
        int rowNumber = 1;
        while ((line = reader.readLine()) != null) {
            rowNumber++;
            if (line.isBlank()) {
                continue;
            }

            List<String> values = parseLine(line, delimiter);
            if (values.isEmpty()) {
                continue;
            }

            ExcelLeadRowDTO dto = new ExcelLeadRowDTO();
            dto.setRowNumber(rowNumber);

            dto.setId(getValue(values, headerMap, "id"));
            dto.setCreatedTime(getValue(values, headerMap, "created_time"));
            dto.setAdId(getValue(values, headerMap, "ad_id"));
            dto.setAdName(getValue(values, headerMap, "ad_name"));
            dto.setAdsetId(getValue(values, headerMap, "adset_id"));
            dto.setAdsetName(getValue(values, headerMap, "adset_name"));
            dto.setCampaignId(getValue(values, headerMap, "campaign_id"));
            dto.setCampaignName(getValue(values, headerMap, "campaign_name"));
            dto.setFormId(getValue(values, headerMap, "form_id"));
            dto.setFormName(getValue(values, headerMap, "form_name"));
            dto.setIsOrganic(getValue(values, headerMap, "is_organic"));
            dto.setPlatform(getValue(values, headerMap, "platform"));

            dto.setMattressType(getValue(values, headerMap, "what_type_of_mattress_are_you_looking_for?", "mattress_type"));
            dto.setMattressSize(getValue(values, headerMap, "what_mattress_size_do_you_need?", "mattress_size"));
            dto.setPreferredBudget(getValue(values, headerMap, "what_is_your_preferred_budget?", "preferred_budget", "budget"));
            dto.setPlanningToBuy(getValue(values, headerMap, "when_are_you_planning_to_buy?", "planning_to_buy", "timeline"));
            dto.setLocation(getValue(values, headerMap, "where_are_you_located?", "location", "city"));

            dto.setFullName(getValue(values, headerMap, "full_name", "name"));
            dto.setPhoneNumber(getValue(values, headerMap, "phone_number", "phone"));
            dto.setLeadStatus(getValue(values, headerMap, "lead_status", "status"));

            if (dto.getId() != null && !dto.getId().isBlank()) {
                rows.add(dto);
            }
        }

        LOG.info("Successfully parsed {} rows from CSV/TSV input", rows.size());
        return rows;
    }

    private String getValue(List<String> values, Map<String, Integer> headerMap, String... candidateHeaders) {
        for (String candidate : candidateHeaders) {
            String norm = normalizeHeader(candidate);
            Integer idx = headerMap.get(norm);
            if (idx != null && idx < values.size()) {
                return values.get(idx).trim();
            }
        }
        return "";
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    private List<String> parseLine(String line, char delimiter) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens;
    }
}
