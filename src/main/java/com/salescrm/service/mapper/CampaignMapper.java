package com.salescrm.service.mapper;

import com.salescrm.domain.Campaign;
import com.salescrm.service.dto.CampaignDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Campaign} and its DTO {@link CampaignDTO}.
 */
@Mapper(componentModel = "spring")
public interface CampaignMapper extends EntityMapper<CampaignDTO, Campaign> {}
