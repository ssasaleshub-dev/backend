package com.salescrm.domain;

import static com.salescrm.domain.LeadPreferenceTestSamples.*;
import static com.salescrm.domain.LeadTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LeadPreferenceTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(LeadPreference.class);
        LeadPreference leadPreference1 = getLeadPreferenceSample1();
        LeadPreference leadPreference2 = new LeadPreference();
        assertThat(leadPreference1).isNotEqualTo(leadPreference2);

        leadPreference2.setId(leadPreference1.getId());
        assertThat(leadPreference1).isEqualTo(leadPreference2);

        leadPreference2 = getLeadPreferenceSample2();
        assertThat(leadPreference1).isNotEqualTo(leadPreference2);
    }

    @Test
    void leadTest() {
        LeadPreference leadPreference = getLeadPreferenceRandomSampleGenerator();
        Lead leadBack = getLeadRandomSampleGenerator();

        leadPreference.setLead(leadBack);
        assertThat(leadPreference.getLead()).isEqualTo(leadBack);

        leadPreference.lead(null);
        assertThat(leadPreference.getLead()).isNull();
    }
}
