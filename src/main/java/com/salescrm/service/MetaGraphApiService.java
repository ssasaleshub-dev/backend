package com.salescrm.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.salescrm.service.dto.ExcelLeadRowDTO;
import com.salescrm.service.dto.meta.MetaWebhookPayload;
import com.salescrm.service.impl.LeadImportServiceImpl;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Enterprise Service for fetching Meta (Facebook & Instagram) Leads
 * directly via Meta Graph API and ingesting them into the CRM.
 */
@Service
public class MetaGraphApiService {

    private static final Logger LOG = LoggerFactory.getLogger(MetaGraphApiService.class);

    @Value("${meta.graph.page-access-token:}")
    private String pageAccessToken;

    @Value("${meta.graph.api-version:v19.0}")
    private String apiVersion;

    private final WebClient webClient;
    private final LeadImportServiceImpl leadImportService;

    public MetaGraphApiService(WebClient.Builder webClientBuilder, LeadImportServiceImpl leadImportService) {
        this.webClient = webClientBuilder.baseUrl("https://graph.facebook.com").build();
        this.leadImportService = leadImportService;
    }

    /**
     * Ingests all leadgen events received from a Meta Webhook payload.
     */
    public Mono<Void> processWebhookPayload(MetaWebhookPayload payload) {
        if (payload == null || payload.getEntry() == null || payload.getEntry().isEmpty()) {
            LOG.warn("Meta Webhook received with empty entry list");
            return Mono.empty();
        }

        return Flux.fromIterable(payload.getEntry())
            .filter(entry -> entry.getChanges() != null)
            .flatMap(entry -> Flux.fromIterable(entry.getChanges()))
            .filter(change -> "leadgen".equalsIgnoreCase(change.getField()) && change.getValue() != null)
            .map(change -> change.getValue().getLeadgenId())
            .filter(leadgenId -> leadgenId != null && !leadgenId.isBlank())
            .concatMap(this::fetchAndIngestLead)
            .then();
    }

    /**
     * Calls Meta Graph API to retrieve complete lead details by leadgen_id
     * and persists the lead into the database.
     */
    public Mono<Void> fetchAndIngestLead(String leadgenId) {
        LOG.info("Fetching lead details from Meta Graph API for leadgen_id: {}", leadgenId);

        if (pageAccessToken == null || pageAccessToken.isBlank()) {
            LOG.warn("Meta Page Access Token is not configured. Falling back to test lead ingestion for leadgen_id: {}", leadgenId);
            ExcelLeadRowDTO mockDto = createMockLead(leadgenId);
            return leadImportService.processSingleRow(mockDto).then();
        }

        String uri = String.format("/%s/%s?access_token=%s", apiVersion, leadgenId, pageAccessToken);

        return webClient.get()
            .uri(uri)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .flatMap(this::mapAndSaveLead)
            .onErrorResume(ex -> {
                LOG.error("Failed to fetch or ingest Meta lead (id={}): {}", leadgenId, ex.getMessage(), ex);
                return Mono.empty();
            });
    }

    private Mono<Void> mapAndSaveLead(JsonNode json) {
        try {
            ExcelLeadRowDTO dto = new ExcelLeadRowDTO();
            String leadId = json.path("id").asText();
            dto.setId(leadId);

            String createdTime = json.path("created_time").asText();
            dto.setCreatedTime(createdTime != null && !createdTime.isBlank() ? createdTime : Instant.now().toString());

            dto.setAdId(json.path("ad_id").asText(null));
            dto.setAdName(json.path("ad_name").asText(null));
            dto.setAdsetId(json.path("adset_id").asText(null));
            dto.setAdsetName(json.path("adset_name").asText(null));
            dto.setCampaignId(json.path("campaign_id").asText(null));
            dto.setCampaignName(json.path("campaign_name").asText(null));
            dto.setFormId(json.path("form_id").asText(null));
            dto.setFormName(json.path("form_name").asText(null));

            // Meta tags organic leads with is_organic = true
            boolean isOrganic = json.path("is_organic").asBoolean(false);
            dto.setIsOrganic(isOrganic ? "true" : "false");

            // Platform is either "fb" or "ig"
            String platform = json.path("platform").asText("FB").toUpperCase();
            dto.setPlatform(platform);

            // Extract fields from field_data array
            JsonNode fieldData = json.path("field_data");
            if (fieldData.isArray()) {
                String firstName = null;
                String lastName = null;

                for (JsonNode field : fieldData) {
                    String name = field.path("name").asText("").toLowerCase().trim();
                    JsonNode values = field.path("values");
                    String val = (values.isArray() && !values.isEmpty()) ? values.get(0).asText("") : "";

                    if (val == null || val.isBlank()) {
                        continue;
                    }

                    if (name.contains("full_name") || name.equals("name") || name.equals("customer_name")) {
                        dto.setFullName(val);
                    } else if (name.contains("first_name")) {
                        firstName = val;
                    } else if (name.contains("last_name")) {
                        lastName = val;
                    } else if (name.contains("phone")) {
                        dto.setPhoneNumber(val);
                    } else if (name.contains("email")) {
                        dto.setEmail(val);
                    } else if (name.contains("city") || name.equals("location")) {
                        dto.setCity(val);
                        dto.setLocation(val);
                    } else if (name.contains("cgpa") || name.contains("percentage")) {
                        dto.setCgpaOrPercentage(val);
                    } else if (name.contains("status") || name.contains("qualification")) {
                        dto.setCurrentStatus(val);
                        dto.setEducationalQualification(val);
                    } else if (name.contains("mattress_type")) {
                        dto.setMattressType(val);
                    } else if (name.contains("mattress_size")) {
                        dto.setMattressSize(val);
                    } else if (name.contains("budget")) {
                        dto.setPreferredBudget(val);
                    } else if (name.contains("planning")) {
                        dto.setPlanningToBuy(val);
                    }
                }

                if ((dto.getFullName() == null || dto.getFullName().isBlank()) && (firstName != null || lastName != null)) {
                    dto.setFullName(((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim());
                }
            }

            if (dto.getFullName() == null || dto.getFullName().isBlank()) {
                dto.setFullName("Meta Lead " + leadId);
            }

            LOG.info("Processing Meta Lead into database: ID={}, Name={}, Phone={}, Platform={}, Organic={}",
                dto.getId(), dto.getFullName(), dto.getPhoneNumber(), dto.getPlatform(), dto.getIsOrganic());

            return leadImportService.processSingleRow(dto).then();
        } catch (Exception e) {
            LOG.error("Failed to map Meta JSON to ExcelLeadRowDTO: {}", e.getMessage(), e);
            return Mono.empty();
        }
    }

    private ExcelLeadRowDTO createMockLead(String leadgenId) {
        ExcelLeadRowDTO dto = new ExcelLeadRowDTO();
        dto.setId(leadgenId);
        dto.setCreatedTime(Instant.now().toString());
        dto.setFullName("Test Lead " + leadgenId);
        dto.setPhoneNumber("+919999999999");
        dto.setEmail("test" + leadgenId + "@example.com");
        dto.setPlatform("FB");
        dto.setIsOrganic("true");
        dto.setFormName("SSA Test Form");
        return dto;
    }
}

