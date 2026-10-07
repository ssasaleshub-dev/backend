package com.salescrm.service.mapper;

import com.salescrm.domain.Lead;
import com.salescrm.domain.LeadPreference;
import com.salescrm.service.dto.LeadDTO;
import com.salescrm.service.dto.LeadPreferenceDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link LeadPreference} and its DTO {@link LeadPreferenceDTO}.
 */
@Mapper(componentModel = "spring")
public interface LeadPreferenceMapper extends EntityMapper<LeadPreferenceDTO, LeadPreference> {
    @Mapping(target = "lead", source = "lead", qualifiedByName = "leadExternalId")
    LeadPreferenceDTO toDto(LeadPreference s);

    @Named("leadExternalId")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "externalId", source = "externalId")
    LeadDTO toDtoLeadExternalId(Lead lead);
}
