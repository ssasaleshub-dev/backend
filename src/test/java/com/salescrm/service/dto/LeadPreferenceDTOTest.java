package com.salescrm.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LeadPreferenceDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(LeadPreferenceDTO.class);
        LeadPreferenceDTO leadPreferenceDTO1 = new LeadPreferenceDTO();
        leadPreferenceDTO1.setId(1L);
        LeadPreferenceDTO leadPreferenceDTO2 = new LeadPreferenceDTO();
        assertThat(leadPreferenceDTO1).isNotEqualTo(leadPreferenceDTO2);
        leadPreferenceDTO2.setId(leadPreferenceDTO1.getId());
        assertThat(leadPreferenceDTO1).isEqualTo(leadPreferenceDTO2);
        leadPreferenceDTO2.setId(2L);
        assertThat(leadPreferenceDTO1).isNotEqualTo(leadPreferenceDTO2);
        leadPreferenceDTO1.setId(null);
        assertThat(leadPreferenceDTO1).isNotEqualTo(leadPreferenceDTO2);
    }
}
