package com.salescrm.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetaAdsCsvIngestionStrategyTest {

    private MetaAdsCsvIngestionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MetaAdsCsvIngestionStrategy();
    }

    @Test
    void shouldSupportCsvAndTsvFormats() {
        assertThat(strategy.supports("leads.csv", "text/csv")).isTrue();
        assertThat(strategy.supports("export.tsv", "text/tab-separated-values")).isTrue();
        assertThat(strategy.supports("data.txt", "text/plain")).isTrue();
        assertThat(strategy.supports("sheet.xlsx", "application/vnd.ms-excel")).isFalse();
    }

    @Test
    void shouldParseStandardCsvWithQuotesAndEscapes() throws Exception {
        String csv =
            "id,created_time,full_name,email,phone_number,city,campaign_name\n" +
            "\"l:12345\",\"2026-05-28T04:22:59-05:00\",\"Praveen, Selvaraj\",\"praveen@gmail.com\",\"p:+916385888585\",\"Rasipuram\",\"Work visa General\"\n" +
            "l:67890,2026-05-28T05:00:00Z,\"He said \"\"Hello\"\"\",test@gmail.com,+918489029053,Namakkal,\"Study visa UK\"\n";

        InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<ExcelLeadRowDTO> rows = strategy.parse(in);

        assertThat(rows).hasSize(2);

        ExcelLeadRowDTO row1 = rows.get(0);
        assertThat(row1.getId()).isEqualTo("l:12345");
        assertThat(row1.getFullName()).isEqualTo("Praveen, Selvaraj");
        assertThat(row1.getEmail()).isEqualTo("praveen@gmail.com");
        assertThat(row1.getPhoneNumber()).isEqualTo("p:+916385888585");
        assertThat(row1.getCity()).isEqualTo("Rasipuram");
        assertThat(row1.getVisaType()).isEqualTo("WORK_VISA");

        ExcelLeadRowDTO row2 = rows.get(1);
        assertThat(row2.getId()).isEqualTo("l:67890");
        assertThat(row2.getFullName()).isEqualTo("He said \"Hello\"");
        assertThat(row2.getVisaType()).isEqualTo("STUDY_VISA");
    }

    @Test
    void shouldStripUtf8BomHeader() throws Exception {
        String csvWithBom =
            "\uFEFFid,created_time,full_name,phone_number\n" +
            "l:999,2026-05-28T04:22:59Z,John Doe,+1234567890\n";

        InputStream in = new ByteArrayInputStream(csvWithBom.getBytes(StandardCharsets.UTF_8));
        List<ExcelLeadRowDTO> rows = strategy.parse(in);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo("l:999");
        assertThat(rows.get(0).getFullName()).isEqualTo("John Doe");
    }

    @Test
    void shouldConcatenateFirstAndLastNameIfFullNameMissing() throws Exception {
        String csv =
            "lead_id,created_at,first_name,last_name,phone\n" +
            "l:111,2026-05-28T04:22:59Z,Kavitha,Ramesh,+919842104521\n";

        InputStream in = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        List<ExcelLeadRowDTO> rows = strategy.parse(in);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo("l:111");
        assertThat(rows.get(0).getFullName()).isEqualTo("Kavitha Ramesh");
    }

    @Test
    void shouldParseClasspathLeadsCsv() throws Exception {
        InputStream in = getClass().getResourceAsStream("/data/leads_data.csv");
        assertThat(in).isNotNull();

        List<ExcelLeadRowDTO> rows = strategy.parse(in);
        assertThat(rows).hasSize(4);
        assertThat(rows.get(0).getId()).isEqualTo("l:4236051776682026");
        assertThat(rows.get(0).getFullName()).isEqualTo("Praveen Selvaraj");
        assertThat(rows.get(0).getVisaType()).isEqualTo("WORK_VISA");
    }

    @Test
    void shouldParseActualGoogleSheetCsv() throws Exception {
        InputStream in = getClass().getResourceAsStream("/data/meta_ads_google_sheet.csv");
        assertThat(in).isNotNull();

        List<ExcelLeadRowDTO> rows = strategy.parse(in);
        assertThat(rows).hasSize(10);

        ExcelLeadRowDTO first = rows.get(0);
        assertThat(first.getId()).isEqualTo("1853515339157752");
        assertThat(first.getPhoneNumber()).contains("8220699044");
        assertThat(first.getEmail()).isEqualTo("jpoovarasan63@gmail.com");
        assertThat(first.getCity()).isEqualTo("Coimbatore");
        assertThat(first.getCurrentStatus()).isEqualTo("completed_ug");
        assertThat(first.getCgpaOrPercentage()).contains("65");
        assertThat(first.getVisaType()).isEqualTo("WORK_VISA");
    }
}
