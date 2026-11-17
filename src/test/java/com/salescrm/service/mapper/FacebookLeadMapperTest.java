package com.salescrm.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.service.dto.ExcelLeadRowDTO;
import com.salescrm.service.dto.facebook.FacebookFieldDataDTO;
import com.salescrm.service.dto.facebook.FacebookLeadGraphResponseDTO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacebookLeadMapperTest {

    private FacebookLeadMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new FacebookLeadMapper();
    }

    @Test
    void shouldMapCompleteGraphResponseToRowDTO() {
        FacebookLeadGraphResponseDTO response = new FacebookLeadGraphResponseDTO();
        response.setId("12020000000003");
        response.setCreatedTime("2026-11-14T17:50:00+0000");
        response.setAdId("12020000000001");
        response.setAdName("Autumn Ad");
        response.setAdsetId("12020000000004");
        response.setAdsetName("Target Audience 25-35");
        response.setCampaignId("12020000000005");
        response.setCampaignName("General Campaign");
        response.setFormId("12020000000002");
        response.setIsOrganic(false);
        response.setPlatform("fb");

        response.setFieldData(List.of(
            new FacebookFieldDataDTO("full_name", List.of("Anand Kumar")),
            new FacebookFieldDataDTO("phone_number", List.of("+919876543210")),
            new FacebookFieldDataDTO("email", List.of("anand.kumar@example.com")),
            new FacebookFieldDataDTO("city", List.of("Coimbatore")),
            new FacebookFieldDataDTO("visa_type", List.of("WORK_VISA")),
            new FacebookFieldDataDTO("age", List.of("26 - 35_years")),
            new FacebookFieldDataDTO("educational_qualification", List.of("Bachelor Degree")),
            new FacebookFieldDataDTO("are_you_interested_in_visa", List.of("yes")),
            new FacebookFieldDataDTO("are_you_currently_working", List.of("yes"))
        ));

        ExcelLeadRowDTO row = mapper.toRowDTO(response, "106540352242522");

        assertThat(row).isNotNull();
        assertThat(row.getId()).isEqualTo("12020000000003");
        assertThat(row.getFullName()).isEqualTo("Anand Kumar");
        assertThat(row.getPhoneNumber()).isEqualTo("+919876543210");
        assertThat(row.getEmail()).isEqualTo("anand.kumar@example.com");
        assertThat(row.getCity()).isEqualTo("Coimbatore");
        assertThat(row.getPlatform()).isEqualTo("FB");
        assertThat(row.getVisaType()).isEqualTo("WORK_VISA");
        assertThat(row.getAge()).isEqualTo("26 - 35_years");
        assertThat(row.getEducationalQualification()).isEqualTo("Bachelor Degree");
        assertThat(row.getAreYouInterested()).isEqualTo("yes");
        assertThat(row.getAreYouWorking()).isEqualTo("yes");
    }

    @Test
    void shouldCombineFirstAndLastNameWhenFullNameIsAbsent() {
        FacebookLeadGraphResponseDTO response = new FacebookLeadGraphResponseDTO();
        response.setId("12020000000004");
        response.setFieldData(List.of(
            new FacebookFieldDataDTO("first_name", List.of("Praveen")),
            new FacebookFieldDataDTO("last_name", List.of("Selvaraj")),
            new FacebookFieldDataDTO("phone_number", List.of("+916385888585"))
        ));

        ExcelLeadRowDTO row = mapper.toRowDTO(response, "106540352242522");

        assertThat(row).isNotNull();
        assertThat(row.getFullName()).isEqualTo("Praveen Selvaraj");
        assertThat(row.getPhoneNumber()).isEqualTo("+916385888585");
    }
}
