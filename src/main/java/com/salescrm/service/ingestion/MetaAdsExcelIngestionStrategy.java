package com.salescrm.service.ingestion;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Strategy implementation for parsing Microsoft Excel (.xlsx, .xls) files
 * containing Meta (Facebook/Instagram) Lead Ads exports.
 * Uses Apache POI for high-fidelity spreadsheet extraction.
 */
@Component
public class MetaAdsExcelIngestionStrategy implements LeadIngestionStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(MetaAdsExcelIngestionStrategy.class);

    private final DataFormatter dataFormatter = new DataFormatter();

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName != null) {
            String lower = fileName.toLowerCase();
            if (lower.endsWith(".xlsx") || lower.endsWith(".xls")) {
                return true;
            }
        }
        if (contentType != null) {
            String lowerType = contentType.toLowerCase();
            return lowerType.contains("spreadsheetml") || lowerType.contains("ms-excel") || lowerType.contains("excel");
        }
        return false;
    }

    @Override
    public List<ExcelLeadRowDTO> parse(InputStream inputStream) throws Exception {
        List<ExcelLeadRowDTO> rows = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return rows;
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                return rows;
            }

            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String headerName = normalizeHeader(dataFormatter.formatCellValue(cell));
                if (!headerName.isEmpty()) {
                    headerMap.put(headerName, cell.getColumnIndex());
                }
            }

            LOG.debug("Detected Excel headers ({} columns): {}", headerMap.size(), headerMap.keySet());

            int rowCount = sheet.getLastRowNum();
            for (int r = 1; r <= rowCount; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                ExcelLeadRowDTO dto = new ExcelLeadRowDTO();
                dto.setRowNumber(r + 1);

                dto.setId(getCellValue(row, headerMap, "id", "lead_id"));
                dto.setCreatedTime(getCellValue(row, headerMap, "created_time", "created_at", "time", "date"));
                dto.setAdId(getCellValue(row, headerMap, "ad_id", "ad id"));
                dto.setAdName(getCellValue(row, headerMap, "ad_name", "ad name"));
                dto.setAdsetId(getCellValue(row, headerMap, "adset_id", "ad_set_id", "adset id", "ad set id"));
                dto.setAdsetName(getCellValue(row, headerMap, "adset_name", "ad_set_name", "adset name", "ad set name"));
                dto.setCampaignId(getCellValue(row, headerMap, "campaign_id", "campaign id"));
                dto.setCampaignName(getCellValue(row, headerMap, "campaign_name", "campaign name"));
                dto.setFormId(getCellValue(row, headerMap, "form_id", "form id"));
                dto.setFormName(getCellValue(row, headerMap, "form_name", "form name"));
                dto.setIsOrganic(getCellValue(row, headerMap, "is_organic", "is organic", "organic"));
                dto.setPlatform(getCellValue(row, headerMap, "platform", "publisher_platform"));

                // Contact info with first/last name fallback
                String fullName = getCellValue(row, headerMap, "full_name", "name", "customer_name");
                if (fullName == null || fullName.isBlank()) {
                    String firstName = getCellValue(row, headerMap, "first_name");
                    String lastName = getCellValue(row, headerMap, "last_name");
                    fullName = (firstName + " " + lastName).trim();
                }
                dto.setFullName(fullName);
                dto.setEmail(getCellValue(row, headerMap, "email", "email_address"));
                dto.setPhoneNumber(getCellValue(row, headerMap, "phone_number", "phone", "phone_no", "mobile_number", "contact_number"));
                dto.setLeadStatus(getCellValue(row, headerMap, "lead_status", "status"));

                // City / Location
                String cityVal = getCellValue(row, headerMap, "city", "where_are_you_located?", "location", "state");
                dto.setCity(cityVal);
                dto.setLocation(cityVal);

                // Visa & Education details
                String explicitVisa = getCellValue(row, headerMap, "visa_type", "visa type", "visa", "category");
                if (explicitVisa != null && !explicitVisa.isBlank()) {
                    dto.setVisaType(explicitVisa);
                } else {
                    dto.setVisaType(inferVisaType(dto.getCampaignName(), dto.getAdName(), dto.getFormName(), dto.getAdsetName()));
                }

                dto.setAge(getCellValue(row, headerMap, "age"));
                dto.setCurrentStatus(getCellValue(row, headerMap, "what_is_your_current_status?", "what_is_your_current_status", "current_status"));
                dto.setCgpaOrPercentage(getCellValue(row, headerMap, "what_is_your_cgpa_or_current_percentage?", "what_is_your_cgpa_or_current_percentage", "cgpa", "percentage"));
                dto.setEducationalQualification(getCellValue(row, headerMap, "educational_qualification", "education", "qualification"));
                dto.setAreYouInterested(getCellValue(row, headerMap, "are_you_interested_in_visa", "are_you_interested", "are_you_looking"));
                dto.setAreYouWorking(getCellValue(row, headerMap, "are_you_currently_working", "are_you_working"));
                dto.setLeadConfirm(getCellValue(row, headerMap, "lead_confirm", "lead confirm", "confirm"));

                // Legacy mattress fields (backward compatibility)
                dto.setMattressType(getCellValue(row, headerMap, "what_type_of_mattress_are_you_looking_for?", "mattress_type"));
                dto.setMattressSize(getCellValue(row, headerMap, "what_mattress_size_do_you_need?", "mattress_size"));
                dto.setPreferredBudget(getCellValue(row, headerMap, "what_is_your_preferred_budget?", "preferred_budget", "budget"));
                dto.setPlanningToBuy(getCellValue(row, headerMap, "when_are_you_planning_to_buy?", "planning_to_buy", "timeline"));

                rows.add(dto);
            }
        }

        LOG.info("Successfully parsed {} rows from Excel workbook", rows.size());
        return rows;
    }

    private String getCellValue(Row row, Map<String, Integer> headerMap, String... candidateHeaders) {
        for (String candidate : candidateHeaders) {
            String normalizedCandidate = normalizeHeader(candidate);
            Integer colIdx = headerMap.get(normalizedCandidate);
            if (colIdx != null) {
                Cell cell = row.getCell(colIdx);
                if (cell != null) {
                    if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        Date date = cell.getDateCellValue();
                        if (date != null) {
                            return date.toInstant().toString();
                        }
                    }
                    return dataFormatter.formatCellValue(cell).trim();
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

    private boolean isRowEmpty(Row row) {
        if (row == null || row.getFirstCellNum() < 0) {
            return true;
        }
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !dataFormatter.formatCellValue(cell).trim().isEmpty()) {
                return false;
            }
        }
        return true;
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

