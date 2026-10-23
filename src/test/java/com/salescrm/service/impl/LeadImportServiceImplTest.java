package com.salescrm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.salescrm.domain.*;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import com.salescrm.repository.*;
import com.salescrm.service.dto.ExcelLeadRowDTO;
import com.salescrm.service.dto.LeadImportResultDTO;
import com.salescrm.service.ingestion.IngestionStrategyFactory;
import com.salescrm.service.ingestion.LeadIngestionStrategy;
import com.salescrm.service.util.DateTimeParser;
import com.salescrm.service.util.PhoneNormalizer;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LeadImportServiceImplTest {

    @Mock
    private IngestionStrategyFactory strategyFactory;

    @Mock
    private LeadIngestionStrategy ingestionStrategy;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private AdSetRepository adSetRepository;

    @Mock
    private AdRepository adRepository;

    @Mock
    private LeadFormRepository leadFormRepository;

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadPreferenceRepository leadPreferenceRepository;

    private PhoneNormalizer phoneNormalizer = new PhoneNormalizer();
    private DateTimeParser dateTimeParser = new DateTimeParser();

    private LeadImportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LeadImportServiceImpl(
            strategyFactory,
            customerRepository,
            campaignRepository,
            adSetRepository,
            adRepository,
            leadFormRepository,
            leadRepository,
            leadPreferenceRepository,
            phoneNormalizer,
            dateTimeParser
        );
    }

    @Test
    void shouldSuccessfullyImportNewLead() {
        ExcelLeadRowDTO row = new ExcelLeadRowDTO();
        row.setRowNumber(2);
        row.setId("l:4236051776682026");
        row.setCreatedTime("2026-05-28T04:22:59-05:00");
        row.setCampaignId("c:12024583561");
        row.setCampaignName("Work visa General");
        row.setAdsetId("as:1202487421");
        row.setAdsetName("Work visa General");
        row.setAdId("ag:1202487421");
        row.setAdName("Work visa general");
        row.setFormId("f:14990446083");
        row.setFormName("Work visa general");
        row.setPlatform("ig");
        row.setFullName("Praveen Selvaraj");
        row.setEmail("praveenselvaraj@gmail.com");
        row.setPhoneNumber("p:+916385888585");
        row.setCity("Rasipuram");
        row.setVisaType("WORK_VISA");
        row.setLeadStatus("CREATED");

        Customer savedCustomer = new Customer();
        savedCustomer.setId(10L);
        savedCustomer.setPhoneNumber("+916385888585");

        Campaign savedCampaign = new Campaign();
        savedCampaign.setId(20L);
        savedCampaign.setExternalId("c:12024583561");

        AdSet savedAdSet = new AdSet();
        savedAdSet.setId(30L);
        savedAdSet.setExternalId("as:1202487421");

        Ad savedAd = new Ad();
        savedAd.setId(40L);
        savedAd.setExternalId("ag:1202487421");

        LeadForm savedLeadForm = new LeadForm();
        savedLeadForm.setId(50L);
        savedLeadForm.setExternalId("f:14990446083");

        Lead savedLead = new Lead();
        savedLead.setId(100L);
        savedLead.setExternalId("l:4236051776682026");

        LeadPreference savedPref = new LeadPreference();
        savedPref.setId(200L);
        savedPref.setLead(savedLead);

        when(customerRepository.findByPhoneNumber("+916385888585")).thenReturn(Mono.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(savedCustomer));

        when(campaignRepository.findByExternalId("c:12024583561")).thenReturn(Mono.empty());
        when(campaignRepository.save(any(Campaign.class))).thenReturn(Mono.just(savedCampaign));

        when(adSetRepository.findByExternalId("as:1202487421")).thenReturn(Mono.empty());
        when(adSetRepository.save(any(AdSet.class))).thenReturn(Mono.just(savedAdSet));

        when(adRepository.findByExternalId("ag:1202487421")).thenReturn(Mono.empty());
        when(adRepository.save(any(Ad.class))).thenReturn(Mono.just(savedAd));

        when(leadFormRepository.findByExternalId("f:14990446083")).thenReturn(Mono.empty());
        when(leadFormRepository.save(any(LeadForm.class))).thenReturn(Mono.just(savedLeadForm));

        when(leadRepository.findByExternalId("l:4236051776682026")).thenReturn(Mono.empty());
        when(leadRepository.save(any(Lead.class))).thenReturn(Mono.just(savedLead));

        when(leadPreferenceRepository.findOneByLeadId(100L)).thenReturn(Mono.empty());
        when(leadPreferenceRepository.save(any(LeadPreference.class))).thenReturn(Mono.just(savedPref));

        LeadImportResultDTO result = service.importRows("test.csv", List.of(row)).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotalRowsProcessed()).isEqualTo(1);
        assertThat(result.getSuccessfulRows()).isEqualTo(1);
        assertThat(result.getFailedRows()).isEqualTo(0);
        assertThat(result.getImportedLeadExternalIds()).containsExactly("l:4236051776682026");
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    void shouldReportFailureWhenLeadIdIsMissing() {
        ExcelLeadRowDTO row = new ExcelLeadRowDTO();
        row.setRowNumber(3);
        row.setId("");
        row.setPhoneNumber("+919876543210");

        LeadImportResultDTO result = service.importRows("test.csv", List.of(row)).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotalRowsProcessed()).isEqualTo(1);
        assertThat(result.getSuccessfulRows()).isEqualTo(0);
        assertThat(result.getFailedRows()).isEqualTo(1);
        assertThat(result.getErrors().get(0)).contains("Missing Lead ID");
    }

    @Test
    void shouldReportFailureWhenPhoneNumberIsMissing() {
        ExcelLeadRowDTO row = new ExcelLeadRowDTO();
        row.setRowNumber(4);
        row.setId("l:9999");
        row.setPhoneNumber(null);

        LeadImportResultDTO result = service.importRows("test.csv", List.of(row)).block();

        assertThat(result).isNotNull();
        assertThat(result.getTotalRowsProcessed()).isEqualTo(1);
        assertThat(result.getSuccessfulRows()).isEqualTo(0);
        assertThat(result.getFailedRows()).isEqualTo(1);
        assertThat(result.getErrors().get(0)).contains("Missing Phone Number");
    }

    @Test
    void shouldDeduplicateAndReuseExistingCustomer() {
        ExcelLeadRowDTO row = new ExcelLeadRowDTO();
        row.setRowNumber(2);
        row.setId("l:12345");
        row.setPhoneNumber("+916385888585");
        row.setFullName("Existing User");

        Customer existingCustomer = new Customer();
        existingCustomer.setId(5L);
        existingCustomer.setPhoneNumber("+916385888585");
        existingCustomer.setFullName("Existing User");

        Lead savedLead = new Lead();
        savedLead.setId(77L);
        savedLead.setExternalId("l:12345");

        when(customerRepository.findByPhoneNumber("+916385888585")).thenReturn(Mono.just(existingCustomer));
        when(leadRepository.findByExternalId("l:12345")).thenReturn(Mono.empty());
        when(leadRepository.save(any(Lead.class))).thenReturn(Mono.just(savedLead));
        when(leadPreferenceRepository.findOneByLeadId(77L)).thenReturn(Mono.empty());
        when(leadPreferenceRepository.save(any(LeadPreference.class))).thenReturn(Mono.just(new LeadPreference()));

        LeadImportResultDTO result = service.importRows("test.csv", List.of(row)).block();

        assertThat(result).isNotNull();
        assertThat(result.getSuccessfulRows()).isEqualTo(1);
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void shouldHandleFileParsingFailureGracefully() throws Exception {
        when(strategyFactory.getStrategy(anyString(), anyString())).thenReturn(ingestionStrategy);
        when(ingestionStrategy.parse(any(InputStream.class))).thenThrow(new RuntimeException("Corrupted spreadsheet"));

        InputStream in = new ByteArrayInputStream("bad data".getBytes());
        LeadImportResultDTO result = service.importFile("broken.xlsx", "application/vnd.ms-excel", in).block();

        assertThat(result).isNotNull();
        assertThat(result.getFailedRows()).isEqualTo(1);
        assertThat(result.getErrors().get(0)).contains("File parsing failure: Corrupted spreadsheet");
    }
}
