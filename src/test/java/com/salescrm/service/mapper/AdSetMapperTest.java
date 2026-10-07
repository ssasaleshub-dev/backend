package com.salescrm.service.mapper;

import static com.salescrm.domain.AdSetAsserts.*;
import static com.salescrm.domain.AdSetTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdSetMapperTest {

    private AdSetMapper adSetMapper;

    @BeforeEach
    void setUp() {
        adSetMapper = new AdSetMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAdSetSample1();
        var actual = adSetMapper.toEntity(adSetMapper.toDto(expected));
        assertAdSetAllPropertiesEquals(expected, actual);
    }
}
