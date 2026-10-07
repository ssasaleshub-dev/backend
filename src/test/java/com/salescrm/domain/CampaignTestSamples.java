package com.salescrm.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class CampaignTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static Campaign getCampaignSample1() {
        return new Campaign().id(1L).externalId("externalId1").name("name1").status("status1");
    }

    public static Campaign getCampaignSample2() {
        return new Campaign().id(2L).externalId("externalId2").name("name2").status("status2");
    }

    public static Campaign getCampaignRandomSampleGenerator() {
        return new Campaign()
            .id(longCount.incrementAndGet())
            .externalId(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .status(UUID.randomUUID().toString());
    }
}
