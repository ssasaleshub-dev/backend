package com.salescrm.service.mapper;

import static com.salescrm.domain.AdAsserts.*;
import static com.salescrm.domain.AdTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdMapperTest {

    private AdMapper adMapper;

    @BeforeEach
    void setUp() {
        adMapper = new AdMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getAdSample1();
        var actual = adMapper.toEntity(adMapper.toDto(expected));
        assertAdAllPropertiesEquals(expected, actual);
    }
}
