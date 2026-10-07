package com.salescrm.service.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AdDTOTest {

    @Test
    void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(AdDTO.class);
        AdDTO adDTO1 = new AdDTO();
        adDTO1.setId(1L);
        AdDTO adDTO2 = new AdDTO();
        assertThat(adDTO1).isNotEqualTo(adDTO2);
        adDTO2.setId(adDTO1.getId());
        assertThat(adDTO1).isEqualTo(adDTO2);
        adDTO2.setId(2L);
        assertThat(adDTO1).isNotEqualTo(adDTO2);
        adDTO1.setId(null);
        assertThat(adDTO1).isNotEqualTo(adDTO2);
    }
}
