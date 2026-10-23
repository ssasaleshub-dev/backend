package com.salescrm.service.ingestion;

import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetaAdsExcelIngestionStrategyTest {

    private MetaAdsExcelIngestionStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MetaAdsExcelIngestionStrategy();
    }

    @Test
    void shouldSupportExcelFormats() {
        assertThat(strategy.supports("leads.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).isTrue();
        assertThat(strategy.supports("leads.xls", "application/vnd.ms-excel")).isTrue();
        assertThat(strategy.supports("leads.csv", "text/csv")).isFalse();
    }

    @Test
    void shouldParseClasspathSampleExcelFile() throws Exception {
        InputStream in = getClass().getResourceAsStream("/data/sample_leads.xlsx");
        assertThat(in).isNotNull();

        List<ExcelLeadRowDTO> rows = strategy.parse(in);
        assertThat(rows).isNotEmpty();

        ExcelLeadRowDTO first = rows.get(0);
        assertThat(first.getId()).isNotBlank();
        assertThat(first.getPhoneNumber()).isNotBlank();
        assertThat(first.getFullName()).isNotBlank();
    }

    @Test
    void shouldParseActualGoogleSheetExcel() throws Exception {
        InputStream in = getClass().getResourceAsStream("/data/meta_ads_google_sheet.xlsx");
        assertThat(in).isNotNull();

        List<ExcelLeadRowDTO> rows = strategy.parse(in);
        assertThat(rows).hasSize(10);

        ExcelLeadRowDTO first = rows.get(0);
        assertThat(first.getId()).isEqualTo("1853515339157752");
        assertThat(first.getPhoneNumber()).contains("8220699044");
        assertThat(first.getFullName()).contains("𝙅");
        assertThat(first.getEmail()).isEqualTo("jpoovarasan63@gmail.com");
        assertThat(first.getCity()).isEqualTo("Coimbatore");
        assertThat(first.getCurrentStatus()).isEqualTo("completed_ug");
        assertThat(first.getCgpaOrPercentage()).contains("65");
        assertThat(first.getVisaType()).isEqualTo("WORK_VISA");
    }
}
