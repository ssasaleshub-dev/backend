package com.salescrm.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LeadFormDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(LeadFormDTO.class);
        LeadFormDTO leadFormDTO1 = new LeadFormDTO();
        leadFormDTO1.setId(1L);
        LeadFormDTO leadFormDTO2 = new LeadFormDTO();
        assertThat(leadFormDTO1).isNotEqualTo(leadFormDTO2);
        leadFormDTO2.setId(leadFormDTO1.getId());
        assertThat(leadFormDTO1).isEqualTo(leadFormDTO2);
        leadFormDTO2.setId(2L);
        assertThat(leadFormDTO1).isNotEqualTo(leadFormDTO2);
        leadFormDTO1.setId(null);
        assertThat(leadFormDTO1).isNotEqualTo(leadFormDTO2);
    }
}
