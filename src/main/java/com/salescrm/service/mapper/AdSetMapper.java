package com.salescrm.service.mapper;

import com.salescrm.domain.AdSet;
import com.salescrm.domain.Campaign;
import com.salescrm.service.dto.AdSetDTO;
import com.salescrm.service.dto.CampaignDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link AdSet} and its DTO {@link AdSetDTO}.
 */
@Mapper(componentModel = "spring")
public interface AdSetMapper extends EntityMapper<AdSetDTO, AdSet> {
    @Mapping(target = "campaign", source = "campaign", qualifiedByName = "campaignName")
    AdSetDTO toDto(AdSet s);

    @Named("campaignName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    CampaignDTO toDtoCampaignName(Campaign campaign);
}
