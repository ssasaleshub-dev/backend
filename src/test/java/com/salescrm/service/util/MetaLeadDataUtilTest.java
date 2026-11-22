package com.salescrm.service.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.service.dto.ExcelLeadRowDTO;
import com.salescrm.service.dto.facebook.FacebookFieldDataDTO;
import com.salescrm.service.dto.facebook.FacebookLeadGraphResponseDTO;
import com.salescrm.service.mapper.FacebookLeadMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetaLeadDataUtilTest {

    private final ObjectMapper json = new ObjectMapper();

    private List<Map<String, String>> parse(String s) throws Exception {
        return json.readValue(s, new TypeReference<>() {});
    }

    @Test
    void humanizesMetaQuestionKeys() {
        assertThat(MetaLeadDataUtil.humanizeQuestion("what_is_your_current_status?")).isEqualTo("What is your current status?");
        assertThat(MetaLeadDataUtil.humanizeQuestion("full_name")).isEqualTo("Full name");
    }

    @Test
    void webhookLeadKeepsEveryAnswerRawPayloadAndCurrentStatus() throws Exception {
        FacebookLeadGraphResponseDTO response = new FacebookLeadGraphResponseDTO();
        response.setId("lead-1");
        response.setFieldData(
            List.of(
                new FacebookFieldDataDTO("full_name", List.of("Anand Kumar")),
                new FacebookFieldDataDTO("what_is_your_current_status?", List.of("completed_ug")),
                new FacebookFieldDataDTO("which_country_do_you_prefer?", List.of("Germany", "Canada"))
            )
        );
        response.setRawJson("{\"id\":\"lead-1\"}");

        ExcelLeadRowDTO row = new FacebookLeadMapper().toRowDTO(response, "page-9");

        // The "current status" answer is no longer dropped for webhook leads
        assertThat(row.getEducationalQualification()).isEqualTo("Bachelor Degree / UG Graduate");
        // A question the CRM has no column for is still kept
        List<Map<String, String>> answers = parse(row.getMetaFormResponses());
        assertThat(answers).hasSize(3);
        assertThat(answers.get(2).get("label")).isEqualTo("Which country do you prefer?");
        assertThat(answers.get(2).get("value")).isEqualTo("Germany, Canada");
        assertThat(row.getMetaRawPayload()).isEqualTo("{\"id\":\"lead-1\"}");
    }

    @Test
    void importRowKeepsAnswersInColumnOrderWithoutAttributionColumns() throws Exception {
        Map<String, Integer> headers = Map.of(
            "id", 0,
            "campaign_name", 1,
            "full_name", 2,
            "what_is_your_current_status?", 3,
            "preferred_intake", 4
        );
        String[] cells = { "123", "Work Visa TN", "Kavitha R", "final_year_ug_student", "" };

        List<Map<String, String>> answers = parse(MetaLeadDataUtil.formResponsesFromRow(headers, i -> cells[i]));

        assertThat(answers).extracting(a -> a.get("key")).containsExactly("full_name", "what_is_your_current_status?");
    }
}
