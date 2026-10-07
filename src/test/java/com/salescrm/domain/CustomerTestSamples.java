package com.salescrm.domain;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class CustomerTestSamples {

    private static final Random random = new Random();
    private static final AtomicLong longCount = new AtomicLong(random.nextInt() + (2L * Integer.MAX_VALUE));

    public static Customer getCustomerSample1() {
        return new Customer()
            .id(1L)
            .fullName("fullName1")
            .phoneNumber("phoneNumber1")
            .rawPhoneNumber("rawPhoneNumber1")
            .location("location1");
    }

    public static Customer getCustomerSample2() {
        return new Customer()
            .id(2L)
            .fullName("fullName2")
            .phoneNumber("phoneNumber2")
            .rawPhoneNumber("rawPhoneNumber2")
            .location("location2");
    }

    public static Customer getCustomerRandomSampleGenerator() {
        return new Customer()
            .id(longCount.incrementAndGet())
            .fullName(UUID.randomUUID().toString())
            .phoneNumber(UUID.randomUUID().toString())
            .rawPhoneNumber(UUID.randomUUID().toString())
            .location(UUID.randomUUID().toString());
    }
}
