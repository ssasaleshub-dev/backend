package com.salescrm.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AdSetDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AdSetDTO.class);
        AdSetDTO adSetDTO1 = new AdSetDTO();
        adSetDTO1.setId(1L);
        AdSetDTO adSetDTO2 = new AdSetDTO();
        assertThat(adSetDTO1).isNotEqualTo(adSetDTO2);
        adSetDTO2.setId(adSetDTO1.getId());
        assertThat(adSetDTO1).isEqualTo(adSetDTO2);
        adSetDTO2.setId(2L);
        assertThat(adSetDTO1).isNotEqualTo(adSetDTO2);
        adSetDTO1.setId(null);
        assertThat(adSetDTO1).isNotEqualTo(adSetDTO2);
    }
}
