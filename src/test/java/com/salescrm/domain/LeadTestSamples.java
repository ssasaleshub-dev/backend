package com.salescrm.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class LeadTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static Lead getLeadSample1() {
        return new Lead().id(1L).externalId("externalId1").notes("notes1");
    }

    public static Lead getLeadSample2() {
        return new Lead().id(2L).externalId("externalId2").notes("notes2");
    }

    public static Lead getLeadRandomSampleGenerator() {
        return new Lead().id(longCount.incrementAndGet()).externalId(UUID.randomUUID().toString()).notes(UUID.randomUUID().toString());
    }
}
