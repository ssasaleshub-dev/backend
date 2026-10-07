package com.salescrm.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class AdTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static Ad getAdSample1() {
        return new Ad().id(1L).externalId("externalId1").name("name1").status("status1");
    }

    public static Ad getAdSample2() {
        return new Ad().id(2L).externalId("externalId2").name("name2").status("status2");
    }

    public static Ad getAdRandomSampleGenerator() {
        return new Ad()
            .id(longCount.incrementAndGet())
            .externalId(UUID.randomUUID().toString())
            .name(UUID.randomUUID().toString())
            .status(UUID.randomUUID().toString());
    }
}
