package com.salescrm.service.util;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Utility to generate binary Microsoft Excel (.xlsx) files from marketing lead data.
 */
public final class ExcelGeneratorUtil {

    private ExcelGeneratorUtil() {}

    public static void generateSampleWorkbook(OutputStream outputStream) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Leads");

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            String[] headers = {
                "id", "created_time", "ad_id", "ad_name", "adset_id", "adset_name",
                "campaign_id", "campaign_name", "form_id", "form_name", "is_organic",
                "platform", "what_type_of_mattress_are_you_looking_for?",
                "what_mattress_size_do_you_need?", "what_is_your_preferred_budget?",
                "when_are_you_planning_to_buy?", "where_are_you_located?",
                "full_name", "phone_number", "lead_status"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            String[][] rows = {
                {
                    "l:3229496824107505", "2026-10-05T02:24:13-05:00", "ag:52559644532980", "Video_ad",
                    "as:52559644533180", "Leads_form", "c:52559644532780", "Latax_mattress_Lead_form",
                    "f:2366440540429976", "latax_Mattress_5/10/2026", "false", "ig",
                    "latex_mattress", "queen", "₹5,000_–_₹10,000", "immediately_/_within_7_days",
                    "salem", "MKR EV MOTORS", "p:+917010202931", "CREATED"
                },
                {
                    "l:1126688279719555", "2026-10-05T04:11:38-05:00", "ag:52559644532980", "Video_ad",
                    "as:52559644533180", "Leads_form", "c:52559644532780", "Latax_mattress_Lead_form",
                    "f:2366440540429976", "latax_Mattress_5/10/2026", "false", "fb",
                    "latex_mattress", "queen", "₹10,000_–_₹15,000", "immediately_/_within_7_days",
                    "namakkal", "Nagaraj Lexi", "p:+919750748427", "CREATED"
                },
                {
                    "l:1111406007947029", "2026-10-05T05:19:22-05:00", "ag:52559644532980", "Video_ad",
                    "as:52559644533180", "Leads_form", "c:52559644532780", "Latax_mattress_Lead_form",
                    "f:2366440540429976", "latax_Mattress_5/10/2026", "false", "ig",
                    "latex_mattress", "king", "₹15,000_–_₹25,000", "immediately_/_within_7_days",
                    "namakkal", "CATHERIN MARY", "p:+919731369877", "CREATED"
                }
            };

            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    row.createCell(c).setCellValue(rows[r][c]);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }
    }

    public static void main(String[] args) throws Exception {
        Path rootFile = Path.of("sample_leads.xlsx");
        try (OutputStream fos = Files.newOutputStream(rootFile)) {
            generateSampleWorkbook(fos);
        }

        Path resourceDir = Path.of("src", "main", "resources", "data");
        Files.createDirectories(resourceDir);
        Path resFile = resourceDir.resolve("sample_leads.xlsx");
        try (OutputStream fos = Files.newOutputStream(resFile)) {
            generateSampleWorkbook(fos);
        }

        System.out.println("Generated sample_leads.xlsx successfully at " + rootFile + " and " + resFile);
    }
}
