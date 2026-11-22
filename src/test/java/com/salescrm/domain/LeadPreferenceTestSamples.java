package com.salescrm.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class LeadPreferenceTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static LeadPreference getLeadPreferenceSample1() {
        return new LeadPreference()
            .id(1L)
            .paymentStatus("paymentStatus1")
            .paymentDetails("paymentDetails1")
            .budgetRange("budgetRange1")
            .purchaseTimeline("purchaseTimeline1");
    }

    public static LeadPreference getLeadPreferenceSample2() {
        return new LeadPreference()
            .id(2L)
            .paymentStatus("paymentStatus2")
            .paymentDetails("paymentDetails2")
            .budgetRange("budgetRange2")
            .purchaseTimeline("purchaseTimeline2");
    }

    public static LeadPreference getLeadPreferenceRandomSampleGenerator() {
        return new LeadPreference()
            .id(longCount.incrementAndGet())
            .paymentStatus(UUID.randomUUID().toString())
            .paymentDetails(UUID.randomUUID().toString())
            .budgetRange(UUID.randomUUID().toString())
            .purchaseTimeline(UUID.randomUUID().toString());
    }
}
