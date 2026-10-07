package com.salescrm.service.mapper;

import com.salescrm.domain.LeadForm;
import com.salescrm.service.dto.LeadFormDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link LeadForm} and its DTO {@link LeadFormDTO}.
 */
@Mapper(componentModel = "spring")
public interface LeadFormMapper extends EntityMapper<LeadFormDTO, LeadForm> {}
