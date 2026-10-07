package com.salescrm.domain;

import static com.salescrm.domain.AdSetTestSamples.*;
import static com.salescrm.domain.CampaignTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class AdSetTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(AdSet.class);
        AdSet adSet1 = getAdSetSample1();
        AdSet adSet2 = new AdSet();
        assertThat(adSet1).isNotEqualTo(adSet2);

        adSet2.setId(adSet1.getId());
        assertThat(adSet1).isEqualTo(adSet2);

        adSet2 = getAdSetSample2();
        assertThat(adSet1).isNotEqualTo(adSet2);
    }

    @Test
    void campaignTest() {
        AdSet adSet = getAdSetRandomSampleGenerator();
        Campaign campaignBack = getCampaignRandomSampleGenerator();

        adSet.setCampaign(campaignBack);
        assertThat(adSet.getCampaign()).isEqualTo(campaignBack);

        adSet.campaign(null);
        assertThat(adSet.getCampaign()).isNull();
    }
}
