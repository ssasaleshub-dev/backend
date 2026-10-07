package com.salescrm.service.mapper;

import static com.salescrm.domain.CampaignAsserts.*;
import static com.salescrm.domain.CampaignTestSamples.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CampaignMapperTest {

    private CampaignMapper campaignMapper;

    @BeforeEach
    void setUp() {
        campaignMapper = new CampaignMapperImpl();
    }

    @Test
    void shouldConvertToDtoAndBack() {
        var expected = getCampaignSample1();
        var actual = campaignMapper.toEntity(campaignMapper.toDto(expected));
        assertCampaignAllPropertiesEquals(expected, actual);
    }
}
