package com.salescrm.domain;

import static com.salescrm.domain.AdTestSamples.*;
import static com.salescrm.domain.CustomerTestSamples.*;
import static com.salescrm.domain.LeadFormTestSamples.*;
import static com.salescrm.domain.LeadPreferenceTestSamples.*;
import static com.salescrm.domain.LeadTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LeadTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Lead.class);
        Lead lead1 = getLeadSample1();
        Lead lead2 = new Lead();
        assertThat(lead1).isNotEqualTo(lead2);

        lead2.setId(lead1.getId());
        assertThat(lead1).isEqualTo(lead2);

        lead2 = getLeadSample2();
        assertThat(lead1).isNotEqualTo(lead2);
    }

    @Test
    void customerTest() {
        Lead lead = getLeadRandomSampleGenerator();
        Customer customerBack = getCustomerRandomSampleGenerator();

        lead.setCustomer(customerBack);
        assertThat(lead.getCustomer()).isEqualTo(customerBack);

        lead.customer(null);
        assertThat(lead.getCustomer()).isNull();
    }

    @Test
    void adTest() {
        Lead lead = getLeadRandomSampleGenerator();
        Ad adBack = getAdRandomSampleGenerator();

        lead.setAd(adBack);
        assertThat(lead.getAd()).isEqualTo(adBack);

        lead.ad(null);
        assertThat(lead.getAd()).isNull();
    }

    @Test
    void leadFormTest() {
        Lead lead = getLeadRandomSampleGenerator();
        LeadForm leadFormBack = getLeadFormRandomSampleGenerator();

        lead.setLeadForm(leadFormBack);
        assertThat(lead.getLeadForm()).isEqualTo(leadFormBack);

        lead.leadForm(null);
        assertThat(lead.getLeadForm()).isNull();
    }

    @Test
    void leadPreferenceTest() {
        Lead lead = getLeadRandomSampleGenerator();
        LeadPreference leadPreferenceBack = getLeadPreferenceRandomSampleGenerator();

        lead.setLeadPreference(leadPreferenceBack);
        assertThat(lead.getLeadPreference()).isEqualTo(leadPreferenceBack);
        assertThat(leadPreferenceBack.getLead()).isEqualTo(lead);

        lead.leadPreference(null);
        assertThat(lead.getLeadPreference()).isNull();
        assertThat(leadPreferenceBack.getLead()).isNull();
    }
}
