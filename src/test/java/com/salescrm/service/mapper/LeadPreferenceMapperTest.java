package com.salescrm.service.mapper;

import static com.salescrm.domain.LeadPreferenceAsserts.*;
import static com.salescrm.domain.LeadPreferenceTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeadPreferenceMapperTest {

    private LeadPreferenceMapper leadPreferenceMapper;

    @BeforeEach
    void setUp() {
        leadPreferenceMapper = new LeadPreferenceMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getLeadPreferenceSample1();
        var actual = leadPreferenceMapper.toEntity(leadPreferenceMapper.toDto(expected));
        assertLeadPreferenceAllPropertiesEquals(expected, actual);
    }
}
