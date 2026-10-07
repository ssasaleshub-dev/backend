package com.salescrm.service.mapper;

import static com.salescrm.domain.LeadFormAsserts.*;
import static com.salescrm.domain.LeadFormTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeadFormMapperTest {

    private LeadFormMapper leadFormMapper;

    @BeforeEach
    void setUp() {
        leadFormMapper = new LeadFormMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getLeadFormSample1();
        var actual = leadFormMapper.toEntity(leadFormMapper.toDto(expected));
        assertLeadFormAllPropertiesEquals(expected, actual);
    }
}
