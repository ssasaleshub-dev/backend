package com.salescrm.service.ingestion;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import java.io.InputStream;
import java.util.ArrayList;
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
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".xlsx") || lower.endsWith(".xls");
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
                headerMap.put(headerName, cell.getColumnIndex());
            }

            LOG.debug("Detected Excel headers: {}", headerMap.keySet());

            int rowCount = sheet.getLastRowNum();
            for (int r = 1; r <= rowCount; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                ExcelLeadRowDTO dto = new ExcelLeadRowDTO();
                dto.setRowNumber(r + 1);

                dto.setId(getCellValue(row, headerMap, "id"));
                dto.setCreatedTime(getCellValue(row, headerMap, "created_time"));
                dto.setAdId(getCellValue(row, headerMap, "ad_id"));
                dto.setAdName(getCellValue(row, headerMap, "ad_name"));
                dto.setAdsetId(getCellValue(row, headerMap, "adset_id"));
                dto.setAdsetName(getCellValue(row, headerMap, "adset_name"));
                dto.setCampaignId(getCellValue(row, headerMap, "campaign_id"));
                dto.setCampaignName(getCellValue(row, headerMap, "campaign_name"));
                dto.setFormId(getCellValue(row, headerMap, "form_id"));
                dto.setFormName(getCellValue(row, headerMap, "form_name"));
                dto.setIsOrganic(getCellValue(row, headerMap, "is_organic"));
                dto.setPlatform(getCellValue(row, headerMap, "platform"));

                // Dynamic survey questions matching variations
                dto.setMattressType(getCellValue(row, headerMap, "what_type_of_mattress_are_you_looking_for?", "mattress_type"));
                dto.setMattressSize(getCellValue(row, headerMap, "what_mattress_size_do_you_need?", "mattress_size"));
                dto.setPreferredBudget(getCellValue(row, headerMap, "what_is_your_preferred_budget?", "preferred_budget", "budget"));
                dto.setPlanningToBuy(getCellValue(row, headerMap, "when_are_you_planning_to_buy?", "planning_to_buy", "timeline"));
                dto.setLocation(getCellValue(row, headerMap, "where_are_you_located?", "location", "city"));

                // Customer contacts
                dto.setFullName(getCellValue(row, headerMap, "full_name", "name"));
                dto.setPhoneNumber(getCellValue(row, headerMap, "phone_number", "phone"));
                dto.setLeadStatus(getCellValue(row, headerMap, "lead_status", "status"));

                if (dto.getId() != null && !dto.getId().isBlank()) {
                    rows.add(dto);
                }
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
        return header.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !cell.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
