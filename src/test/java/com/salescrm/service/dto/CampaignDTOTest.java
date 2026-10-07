package com.salescrm.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class CampaignDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(CampaignDTO.class);
        CampaignDTO campaignDTO1 = new CampaignDTO();
        campaignDTO1.setId(1L);
        CampaignDTO campaignDTO2 = new CampaignDTO();
        assertThat(campaignDTO1).isNotEqualTo(campaignDTO2);
        campaignDTO2.setId(campaignDTO1.getId());
        assertThat(campaignDTO1).isEqualTo(campaignDTO2);
        campaignDTO2.setId(2L);
        assertThat(campaignDTO1).isNotEqualTo(campaignDTO2);
        campaignDTO1.setId(null);
        assertThat(campaignDTO1).isNotEqualTo(campaignDTO2);
    }
}
