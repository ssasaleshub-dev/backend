package com.salescrm.domain;

import static com.salescrm.domain.LeadFormTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.salescrm.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class LeadFormTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(LeadForm.class);
        LeadForm leadForm1 = getLeadFormSample1();
        LeadForm leadForm2 = new LeadForm();
        assertThat(leadForm1).isNotEqualTo(leadForm2);

        leadForm2.setId(leadForm1.getId());
        assertThat(leadForm1).isEqualTo(leadForm2);

        leadForm2 = getLeadFormSample2();
        assertThat(leadForm1).isNotEqualTo(leadForm2);
    }
}
