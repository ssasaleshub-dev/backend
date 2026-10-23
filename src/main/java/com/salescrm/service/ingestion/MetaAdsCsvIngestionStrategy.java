package com.salescrm.service.ingestion;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for parsing CSV and TSV lead exports directly from
 * Meta Ads Manager (Facebook / Instagram Lead Ads).
 * Robustly supports RFC 4180 quotes, multiline values, escaped quotes, UTF-8 BOM,
 * and comprehensive header synonyms.
 */
@Component
public class MetaAdsCsvIngestionStrategy implements LeadIngestionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(MetaAdsCsvIngestionStrategy.class);

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".csv") || lower.endsWith(".tsv") || lower.endsWith(".txt")) {
                return true;
            }
        }
        if (contentType != null) {
            String lowerType = contentType.toLowerCase();
            return lowerType.contains("csv") || lowerType.contains("tab-separated") || lowerType.contains("text/plain");
        }
        return false;
    }

    @Override
    public List<ExcelLeadRowDTO> parse(InputStream inputStream) throws Exception {
        List<ExcelLeadRowDTO> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Detect delimiter and parse records
        List<List<String>> records = parseRecords(reader);
        if (records.isEmpty()) {
            return rows;
        }

        List<String> rawHeaders = records.get(0);
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < rawHeaders.size(); i++) {
            String norm = normalizeHeader(rawHeaders.get(i));
            if (!norm.isEmpty()) {
                headerMap.put(norm, i);
            }
        }

        LOG.debug("Detected CSV/TSV headers ({} columns): {}", headerMap.size(), headerMap.keySet());

        int rowNumber = 1; // header was row 1
        for (int r = 1; r < records.size(); r++) {
            rowNumber++;
            List<String> values = records.get(r);
            if (isRecordEmpty(values)) {
                continue;
            }

            ExcelLeadRowDTO dto = new ExcelLeadRowDTO();
            dto.setRowNumber(rowNumber);

            dto.setId(getValue(values, headerMap, "id", "lead_id"));
            dto.setCreatedTime(getValue(values, headerMap, "created_time", "created_at", "time", "date"));
            dto.setAdId(getValue(values, headerMap, "ad_id", "ad id"));
            dto.setAdName(getValue(values, headerMap, "ad_name", "ad name"));
            dto.setAdsetId(getValue(values, headerMap, "adset_id", "ad_set_id", "adset id", "ad set id"));
            dto.setAdsetName(getValue(values, headerMap, "adset_name", "ad_set_name", "adset name", "ad set name"));
            dto.setCampaignId(getValue(values, headerMap, "campaign_id", "campaign id"));
            dto.setCampaignName(getValue(values, headerMap, "campaign_name", "campaign name"));
            dto.setFormId(getValue(values, headerMap, "form_id", "form id"));
            dto.setFormName(getValue(values, headerMap, "form_name", "form name"));
            dto.setIsOrganic(getValue(values, headerMap, "is_organic", "is organic", "organic"));
            dto.setPlatform(getValue(values, headerMap, "platform", "publisher_platform"));

            // Contact info with first/last name fallback
            String fullName = getValue(values, headerMap, "full_name", "customer_name", "contact_name");
            if (fullName == null || fullName.isBlank()) {
                String firstName = getValue(values, headerMap, "first_name", "firstname");
                String lastName = getValue(values, headerMap, "last_name", "lastname");
                fullName = (firstName + " " + lastName).trim();
            }
            dto.setFullName(fullName);
            dto.setEmail(getValue(values, headerMap, "email", "email_address"));
            dto.setPhoneNumber(getValue(values, headerMap, "phone_number", "phone", "phone_no", "mobile_number", "contact_number"));
            dto.setLeadStatus(getValue(values, headerMap, "lead_status", "status"));

            // City / Location
            String cityVal = getValue(values, headerMap, "city", "where_are_you_located?", "location", "state");
            dto.setCity(cityVal);
            dto.setLocation(cityVal);

            // Visa & Education details
            String explicitVisa = getValue(values, headerMap, "visa_type", "visa_desk");
            if (explicitVisa != null && !explicitVisa.isBlank()) {
                dto.setVisaType(explicitVisa);
            } else {
                dto.setVisaType(inferVisaType(dto.getCampaignName(), dto.getAdName(), dto.getFormName(), dto.getAdsetName()));
            }

            dto.setAge(getValue(values, headerMap, "age"));
            dto.setCurrentStatus(getValue(values, headerMap, "what_is_your_current_status?", "what_is_your_current_status", "what_is_your_status?", "current_status"));
            dto.setCgpaOrPercentage(getValue(values, headerMap, "what_is_your_ug_cgpa_or_percentage?", "what_is_your_cgpa_or_current_percentage?", "what_is_your_cgpa_or_percentage?", "ug_cgpa", "cgpa", "percentage"));
            dto.setEducationalQualification(getValue(values, headerMap, "educational_qualification", "education", "qualification"));
            dto.setAreYouInterested(getValue(values, headerMap, "are_you_interested_in_visa", "are_you_interested", "are_you_looking"));
            dto.setAreYouWorking(getValue(values, headerMap, "are_you_currently_working", "are_you_working"));
            dto.setLeadConfirm(getValue(values, headerMap, "lead_confirm", "lead confirm", "confirm"));

            // Legacy mattress fields (backward compatibility)
            dto.setMattressType(getValue(values, headerMap, "what_type_of_mattress_are_you_looking_for?", "mattress_type"));
            dto.setMattressSize(getValue(values, headerMap, "what_mattress_size_do_you_need?", "mattress_size"));
            dto.setPreferredBudget(getValue(values, headerMap, "what_is_your_preferred_budget?", "preferred_budget", "budget"));
            dto.setPlanningToBuy(getValue(values, headerMap, "when_are_you_planning_to_buy?", "planning_to_buy", "timeline"));

            rows.add(dto);
        }

        LOG.info("Successfully parsed {} rows from CSV/TSV input", rows.size());
        return rows;
    }

    private List<List<String>> parseRecords(BufferedReader reader) throws Exception {
        reader.mark(4096);
        String firstLine = reader.readLine();
        reader.reset();

        if (firstLine == null || firstLine.isBlank()) {
            return new ArrayList<>();
        }

        char delimiter = ',';
        if (firstLine.contains("\t")) {
            delimiter = '\t';
        } else if (firstLine.contains(";") && !firstLine.contains(",")) {
            delimiter = ';';
        }

        return readRfc4180Csv(reader, delimiter);
    }

    private List<List<String>> readRfc4180Csv(Reader in, char delimiter) throws Exception {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRecord = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        int r;
        while ((r = in.read()) != -1) {
            char c = (char) r;
            if (c == '\uFEFF') {
                continue; // Strip UTF-8 Byte Order Mark
            }

            if (inQuotes) {
                if (c == '"') {
                    in.mark(1);
                    int next = in.read();
                    if (next == '"') {
                        currentField.append('"');
                    } else {
                        inQuotes = false;
                        if (next != -1) {
                            in.reset();
                        }
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == delimiter) {
                    currentRecord.add(currentField.toString().trim());
                    currentField.setLength(0);
                } else if (c == '\r') {
                    in.mark(1);
                    int next = in.read();
                    if (next != '\n' && next != -1) {
                        in.reset();
                    }
                    currentRecord.add(currentField.toString().trim());
                    currentField.setLength(0);
                    if (!isRecordEmpty(currentRecord)) {
                        records.add(currentRecord);
                    }
                    currentRecord = new ArrayList<>();
                } else if (c == '\n') {
                    currentRecord.add(currentField.toString().trim());
                    currentField.setLength(0);
                    if (!isRecordEmpty(currentRecord)) {
                        records.add(currentRecord);
                    }
                    currentRecord = new ArrayList<>();
                } else {
                    currentField.append(c);
                }
            }
        }

        if (currentField.length() > 0 || !currentRecord.isEmpty()) {
            currentRecord.add(currentField.toString().trim());
            if (!isRecordEmpty(currentRecord)) {
                records.add(currentRecord);
            }
        }

        return records;
    }

    private boolean isRecordEmpty(List<String> record) {
        if (record == null || record.isEmpty()) {
            return true;
        }
        for (String val : record) {
            if (val != null && !val.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private String getValue(List<String> values, Map<String, Integer> headerMap, String... candidateHeaders) {
        for (String candidate : candidateHeaders) {
            String norm = normalizeHeader(candidate);
            Integer idx = headerMap.get(norm);
            if (idx != null && idx < values.size()) {
                String val = values.get(idx);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
        }
        return "";
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        String cleaned = header.replaceAll("^[\\uFEFF\\uFFFE]", "").trim().toLowerCase();
        return cleaned.replaceAll("[\\s\\-\\.]+", "_");
    }

    private String inferVisaType(String... sources) {
        for (String s : sources) {
            if (s != null) {
                String lower = s.toLowerCase();
                if (lower.contains("work")) {
                    return "WORK_VISA";
                }
                if (lower.contains("study") || lower.contains("student")) {
                    return "STUDY_VISA";
                }
                if (lower.contains("domestic") || lower.contains("college") || lower.contains("admission")) {
                    return "DOMESTIC_COLLEGE_ADMISSION";
                }
            }
        }
        return "WORK_VISA";
    }
}

