package com.salescrm.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class LeadFormTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static LeadForm getLeadFormSample1() {
        return new LeadForm().id(1L).externalId("externalId1").name("name1").status("status1");
    }

    public static LeadForm getLeadFormSample2() {
        return new LeadForm().id(2L).externalId("externalId2").name("name2").status("status2");
    }

    public static LeadForm getLeadFormRandomSampleGenerator() {
        return new LeadForm()
            .id(longCount.incrementAndGet())
            .externalId(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .status(UUID.randomUUID().toString());
    }
}
