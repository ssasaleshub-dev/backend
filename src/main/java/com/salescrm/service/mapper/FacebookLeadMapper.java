package com.salescrm.service.mapper;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import com.salescrm.service.dto.facebook.FacebookFieldDataDTO;
import com.salescrm.service.dto.facebook.FacebookLeadGraphResponseDTO;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Maps Meta Graph API responses into normalized ExcelLeadRowDTO instances.
 * Normalizes field names, handles first/last name combinations, maps custom questions,
 * and preserves metadata (e.g. Page ID, unmapped fields).
 */
@Component
public class FacebookLeadMapper {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookLeadMapper.class);

    /**
     * Converts a Meta Graph API response into an ExcelLeadRowDTO for standard ingestion.
     *
     * @param graphResponse response from Meta Graph API
     * @param pageId Meta Facebook Page ID from webhook notification
     * @return normalized ExcelLeadRowDTO
     */
    public ExcelLeadRowDTO toRowDTO(FacebookLeadGraphResponseDTO graphResponse, String pageId) {
        if (graphResponse == null) {
            return null;
        }

        ExcelLeadRowDTO dto = new ExcelLeadRowDTO();
        dto.setId(graphResponse.getId());
        dto.setCreatedTime(graphResponse.getCreatedTime());
        dto.setAdId(graphResponse.getAdId());
        dto.setAdName(graphResponse.getAdName() != null ? graphResponse.getAdName() : "Ad " + graphResponse.getAdId());
        dto.setAdsetId(graphResponse.getAdsetId());
        dto.setAdsetName(graphResponse.getAdsetName() != null ? graphResponse.getAdsetName() : "AdSet " + graphResponse.getAdsetId());
        dto.setCampaignId(graphResponse.getCampaignId());
        dto.setCampaignName(graphResponse.getCampaignName() != null ? graphResponse.getCampaignName() : "Meta Campaign " + graphResponse.getCampaignId());
        dto.setFormId(graphResponse.getFormId());
        dto.setFormName("Instant Form " + (graphResponse.getFormId() != null ? graphResponse.getFormId() : ""));
        dto.setIsOrganic(String.valueOf(graphResponse.getIsOrganic() != null ? graphResponse.getIsOrganic() : false));

        String platform = graphResponse.getPlatform();
        dto.setPlatform(platform != null && !platform.isBlank() ? platform.toUpperCase() : "FB");
        dto.setLeadStatus("CREATED");

        // Index field_data by normalized field name
        Map<String, String> fieldMap = new HashMap<>();
        StringBuilder unmappedNotes = new StringBuilder();

        if (graphResponse.getFieldData() != null) {
            for (FacebookFieldDataDTO field : graphResponse.getFieldData()) {
                if (field.getName() != null && field.getFirstValue() != null) {
                    String rawName = field.getName().trim();
                    String normName = normalizeKey(rawName);
                    String value = field.getFirstValue().trim();
                    fieldMap.put(normName, value);
                }
            }
        }

        // Contact details
        String fullName = getFirstMatching(fieldMap, "full_name", "customer_name", "name", "contact_name");
        String firstName = getFirstMatching(fieldMap, "first_name", "firstname");
        String lastName = getFirstMatching(fieldMap, "last_name", "lastname");

        if (fullName == null || fullName.isBlank()) {
            if (firstName != null || lastName != null) {
                fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
            }
        }
        if (fullName == null || fullName.isBlank()) {
            fullName = "Facebook Lead";
        }
        dto.setFullName(fullName);

        dto.setEmail(getFirstMatching(fieldMap, "email", "email_address"));
        dto.setPhoneNumber(getFirstMatching(fieldMap, "phone_number", "phone", "phone_no", "mobile_number", "contact_number"));

        // Location / City
        String cityVal = getFirstMatching(fieldMap, "city", "where_are_you_located", "location", "state");
        dto.setCity(cityVal);
        dto.setLocation(cityVal);

        // Visa / Education custom questions
        dto.setVisaType(getFirstMatching(fieldMap, "visa_type", "visa_desk", "what_type_of_visa_are_you_looking_for"));
        dto.setAge(getFirstMatching(fieldMap, "age", "what_is_your_age"));
        dto.setEducationalQualification(getFirstMatching(fieldMap, "educational_qualification", "education", "qualification", "highest_qualification"));
        dto.setCurrentStatus(getFirstMatching(fieldMap, "what_is_your_current_status", "current_status", "status"));
        dto.setCgpaOrPercentage(getFirstMatching(fieldMap, "what_is_your_ug_cgpa_or_percentage", "ug_cgpa", "cgpa", "percentage"));
        dto.setAreYouInterested(getFirstMatching(fieldMap, "are_you_interested_in_visa", "are_you_interested", "are_you_looking"));
        dto.setAreYouWorking(getFirstMatching(fieldMap, "are_you_currently_working", "are_you_working"));
        dto.setLeadConfirm(getFirstMatching(fieldMap, "lead_confirm", "confirm"));

        // Product survey custom questions (mattress)
        dto.setMattressType(getFirstMatching(fieldMap, "what_type_of_mattress_are_you_looking_for", "mattress_type"));
        dto.setMattressSize(getFirstMatching(fieldMap, "what_mattress_size_do_you_need", "mattress_size"));
        dto.setPreferredBudget(getFirstMatching(fieldMap, "what_is_your_preferred_budget", "preferred_budget", "budget"));
        dto.setPlanningToBuy(getFirstMatching(fieldMap, "when_are_you_planning_to_buy", "planning_to_buy", "timeline"));

        LOG.debug("Mapped Meta lead {} for customer '{}' ({})", dto.getId(), dto.getFullName(), dto.getPhoneNumber());
        return dto;
    }

    private String getFirstMatching(Map<String, String> fieldMap, String... keys) {
        for (String key : keys) {
            String val = fieldMap.get(key);
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return null;
    }

    private String normalizeKey(String key) {
        if (key == null) return "";
        return key.toLowerCase()
            .replace("?", "")
            .replace(":", "")
            .replace("-", "_")
            .replace(" ", "_")
            .replaceAll("[^a-z0-9_]", "");
    }
}
