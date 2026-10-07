package com.salescrm.domain;

import static com.salescrm.domain.AdSetTestSamples.*;
import static com.salescrm.domain.AdTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AdTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Ad.class);
        Ad ad1 = getAdSample1();
        Ad ad2 = new Ad();
        assertThat(ad1).isNotEqualTo(ad2);

        ad2.setId(ad1.getId());
        assertThat(ad1).isEqualTo(ad2);

        ad2 = getAdSample2();
        assertThat(ad1).isNotEqualTo(ad2);
    }

    @Test
    void adSetTest() {
        Ad ad = getAdRandomSampleGenerator();
        AdSet adSetBack = getAdSetRandomSampleGenerator();

        ad.setAdSet(adSetBack);
        assertThat(ad.getAdSet()).isEqualTo(adSetBack);

        ad.adSet(null);
        assertThat(ad.getAdSet()).isNull();
    }
}
