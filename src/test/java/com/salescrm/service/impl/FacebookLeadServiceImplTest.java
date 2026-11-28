package com.salescrm.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.salescrm.domain.*;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import com.salescrm.repository.*;
import com.salescrm.service.LeadActivityService;
import com.salescrm.service.client.FacebookGraphApiClient;
import com.salescrm.service.dto.facebook.FacebookFieldDataDTO;
import com.salescrm.service.dto.facebook.FacebookLeadGraphResponseDTO;
import com.salescrm.service.dto.facebook.FacebookWebhookPayloadDTO;
import com.salescrm.service.mapper.FacebookLeadMapper;
import com.salescrm.service.util.DateTimeParser;
import com.salescrm.service.util.PhoneNormalizer;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class FacebookLeadServiceImplTest {

    @Mock
    private FacebookGraphApiClient graphApiClient;

    @Mock
    private LeadRepository leadRepository;

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
    private LeadPreferenceRepository leadPreferenceRepository;

    @Mock
    private LeadActivityService leadActivityService;

    private FacebookLeadMapper leadMapper = new FacebookLeadMapper();
    private PhoneNormalizer phoneNormalizer = new PhoneNormalizer();
    private DateTimeParser dateTimeParser = new DateTimeParser();

    private FacebookLeadServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FacebookLeadServiceImpl(
            graphApiClient,
            leadMapper,
            leadRepository,
            customerRepository,
            campaignRepository,
            adSetRepository,
            adRepository,
            leadFormRepository,
            leadPreferenceRepository,
            phoneNormalizer,
            dateTimeParser,
            leadActivityService
        );
    }

    @Test
    void shouldSkipProcessingWhenLeadAlreadyExists() {
        String leadgenId = "12020000000003";
        Lead existingLead = new Lead();
        existingLead.setId(100L);
        existingLead.setExternalId(leadgenId);

        when(leadRepository.findByExternalId(leadgenId)).thenReturn(Mono.just(existingLead));

        Lead result = service.processLead(leadgenId, "page_123", "ad_1", "form_1").block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getExternalId()).isEqualTo(leadgenId);

        // Verify Graph API was NOT invoked
        verify(graphApiClient, never()).getLeadDetails(anyString());
    }

    @Test
    void shouldSuccessfullyRetrieveFromGraphApiAndPersistLead() {
        String leadgenId = "12020000000005";
        when(leadRepository.findByExternalId(leadgenId)).thenReturn(Mono.empty());

        FacebookLeadGraphResponseDTO graphResponse = new FacebookLeadGraphResponseDTO();
        graphResponse.setId(leadgenId);
        graphResponse.setCreatedTime("2026-11-14T12:00:00+0000");
        graphResponse.setCampaignId("c_101");
        graphResponse.setCampaignName("General Campaign");
        graphResponse.setAdsetId("as_201");
        graphResponse.setAdsetName("AdSet 1");
        graphResponse.setAdId("ad_301");
        graphResponse.setAdName("Video Ad");
        graphResponse.setFormId("f_401");
        graphResponse.setIsOrganic(false);
        graphResponse.setPlatform("fb");
        graphResponse.setFieldData(List.of(
            new FacebookFieldDataDTO("full_name", List.of("Kavitha Ramesh")),
            new FacebookFieldDataDTO("phone_number", List.of("+919842104521")),
            new FacebookFieldDataDTO("email", List.of("kavitha.ramesh@gmail.com")),
            new FacebookFieldDataDTO("city", List.of("Salem"))
        ));

        when(graphApiClient.getLeadDetails(leadgenId)).thenReturn(Mono.just(graphResponse));

        // Mock Customer
        when(customerRepository.findByPhoneNumber("+919842104521")).thenReturn(Mono.empty());
        when(customerRepository.findByEmail("kavitha.ramesh@gmail.com")).thenReturn(Mono.empty());
        Customer savedCustomer = new Customer();
        savedCustomer.setId(1L);
        savedCustomer.setPhoneNumber("+919842104521");
        when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(savedCustomer));

        // Mock Campaign, AdSet, Ad, LeadForm
        when(campaignRepository.findByExternalId("c_101")).thenReturn(Mono.empty());
        Campaign savedCampaign = new Campaign();
        savedCampaign.setId(10L);
        when(campaignRepository.save(any(Campaign.class))).thenReturn(Mono.just(savedCampaign));

        when(adSetRepository.findByExternalId("as_201")).thenReturn(Mono.empty());
        AdSet savedAdSet = new AdSet();
        savedAdSet.setId(20L);
        when(adSetRepository.save(any(AdSet.class))).thenReturn(Mono.just(savedAdSet));

        when(adRepository.findByExternalId("ad_301")).thenReturn(Mono.empty());
        Ad savedAd = new Ad();
        savedAd.setId(30L);
        when(adRepository.save(any(Ad.class))).thenReturn(Mono.just(savedAd));

        when(leadFormRepository.findByExternalId("f_401")).thenReturn(Mono.empty());
        when(graphApiClient.getFormName("f_401")).thenReturn(Mono.just("Work Visa Enquiry Form"));
        LeadForm savedLeadForm = new LeadForm();
        savedLeadForm.setId(40L);
        when(leadFormRepository.save(any(LeadForm.class))).thenReturn(Mono.just(savedLeadForm));

        // Mock Lead persistence
        Lead savedLead = new Lead();
        savedLead.setId(500L);
        savedLead.setExternalId(leadgenId);
        savedLead.setStatus(LeadStatus.CREATED);
        savedLead.setPlatform(PlatformType.FB);
        savedLead.setCustomer(savedCustomer);
        when(leadRepository.save(any(Lead.class))).thenReturn(Mono.just(savedLead));

        // Mock LeadPreference
        when(leadPreferenceRepository.findOneByLeadId(500L)).thenReturn(Mono.empty());
        when(leadPreferenceRepository.save(any(LeadPreference.class))).thenReturn(Mono.just(new LeadPreference()));
        when(leadActivityService.logActivity(any())).thenReturn(Mono.empty());

        Lead result = service.processLead(leadgenId, "page_123", "ad_301", "f_401").block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(500L);
        assertThat(result.getExternalId()).isEqualTo(leadgenId);

        verify(leadRepository).save(any(Lead.class));
        verify(customerRepository).save(any(Customer.class));
        // The form is saved under its real Meta name, not an "Instant Form <id>" placeholder
        verify(leadFormRepository).save(org.mockito.ArgumentMatchers.argThat(form -> "Work Visa Enquiry Form".equals(form.getName())));
        // Page ID and the raw form answers are kept on the lead
        verify(leadRepository).save(org.mockito.ArgumentMatchers.argThat(lead -> "page_123".equals(lead.getMetaPageId())));
    }

    @Test
    void shouldUseSecondaryAccountPageAccessTokenForLeadAndFormLookup() {
        com.salescrm.config.FacebookProperties props = new com.salescrm.config.FacebookProperties();
        props.setPageAccessToken("primary_token");
        com.salescrm.config.FacebookProperties.MetaAccountConfig team2 = new com.salescrm.config.FacebookProperties.MetaAccountConfig();
        team2.setName("team2");
        team2.setPageId("103160805770841");
        team2.setPageAccessToken("team2_page_token");
        props.setAccounts(List.of(team2));
        org.springframework.test.util.ReflectionTestUtils.setField(service, "facebookProperties", props);

        String leadgenId = "12020000000099";
        when(leadRepository.findByExternalId(leadgenId)).thenReturn(Mono.empty());

        FacebookLeadGraphResponseDTO graphResponse = new FacebookLeadGraphResponseDTO();
        graphResponse.setId(leadgenId);
        graphResponse.setCreatedTime("2026-11-14T12:00:00+0000");
        graphResponse.setCampaignId("c_201");
        graphResponse.setAdsetId("as_202");
        graphResponse.setAdId("ad_203");
        graphResponse.setFormId("f_204");
        graphResponse.setIsOrganic(false);
        graphResponse.setPlatform("fb");
        graphResponse.setFieldData(List.of(
            new FacebookFieldDataDTO("full_name", List.of("Arun Kumar")),
            new FacebookFieldDataDTO("phone_number", List.of("+919876543210"))
        ));

        when(graphApiClient.getLeadDetails(leadgenId, "team2_page_token")).thenReturn(Mono.just(graphResponse));
        when(graphApiClient.getFormName("f_204", "team2_page_token")).thenReturn(Mono.just("Team 2 Form"));

        Customer savedCustomer = new Customer();
        savedCustomer.setId(2L);
        when(customerRepository.findByPhoneNumber("+919876543210")).thenReturn(Mono.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(Mono.just(savedCustomer));

        Campaign savedCampaign = new Campaign();
        savedCampaign.setId(11L);
        when(campaignRepository.findByExternalId("c_201")).thenReturn(Mono.empty());
        when(campaignRepository.save(any(Campaign.class))).thenReturn(Mono.just(savedCampaign));

        AdSet savedAdSet = new AdSet();
        savedAdSet.setId(21L);
        when(adSetRepository.findByExternalId("as_202")).thenReturn(Mono.empty());
        when(adSetRepository.save(any(AdSet.class))).thenReturn(Mono.just(savedAdSet));

        Ad savedAd = new Ad();
        savedAd.setId(31L);
        when(adRepository.findByExternalId("ad_203")).thenReturn(Mono.empty());
        when(adRepository.save(any(Ad.class))).thenReturn(Mono.just(savedAd));

        LeadForm savedLeadForm = new LeadForm();
        savedLeadForm.setId(41L);
        when(leadFormRepository.findByExternalId("f_204")).thenReturn(Mono.empty());
        when(leadFormRepository.save(any(LeadForm.class))).thenReturn(Mono.just(savedLeadForm));

        Lead savedLead = new Lead();
        savedLead.setId(501L);
        savedLead.setExternalId(leadgenId);
        savedLead.setCustomer(savedCustomer);
        when(leadRepository.save(any(Lead.class))).thenReturn(Mono.just(savedLead));
        when(leadPreferenceRepository.findOneByLeadId(501L)).thenReturn(Mono.empty());
        when(leadPreferenceRepository.save(any(LeadPreference.class))).thenReturn(Mono.just(new LeadPreference()));
        when(leadActivityService.logActivity(any())).thenReturn(Mono.empty());

        Lead result = service.processLead(leadgenId, "103160805770841", "ad_203", "f_204").block();

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(501L);
        verify(graphApiClient).getLeadDetails(leadgenId, "team2_page_token");
        verify(graphApiClient).getFormName("f_204", "team2_page_token");
    }
}
