package com.salescrm.service.mapper;

import com.salescrm.domain.Ad;
import com.salescrm.domain.AdSet;
import com.salescrm.service.dto.AdDTO;
import com.salescrm.service.dto.AdSetDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Ad} and its DTO {@link AdDTO}.
 */
@Mapper(componentModel = "spring")
public interface AdMapper extends EntityMapper<AdDTO, Ad> {
    @Mapping(target = "adSet", source = "adSet", qualifiedByName = "adSetName")
    AdDTO toDto(Ad s);

    @Named("adSetName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    AdSetDTO toDtoAdSetName(AdSet adSet);
}
