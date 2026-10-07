package com.salescrm.service.mapper;

import com.salescrm.domain.Ad;
import com.salescrm.domain.Customer;
import com.salescrm.domain.Lead;
import com.salescrm.domain.LeadForm;
import com.salescrm.service.dto.AdDTO;
import com.salescrm.service.dto.CustomerDTO;
import com.salescrm.service.dto.LeadDTO;
import com.salescrm.service.dto.LeadFormDTO;
import org.mapstruct.*;

/**
 * Mapper for the entity {@link Lead} and its DTO {@link LeadDTO}.
 */
@Mapper(componentModel = "spring")
public interface LeadMapper extends EntityMapper<LeadDTO, Lead> {
    @Mapping(target = "customer", source = "customer", qualifiedByName = "customerFullName")
    @Mapping(target = "ad", source = "ad", qualifiedByName = "adName")
    @Mapping(target = "leadForm", source = "leadForm", qualifiedByName = "leadFormName")
    LeadDTO toDto(Lead s);

    @Named("customerFullName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "fullName", source = "fullName")
    CustomerDTO toDtoCustomerFullName(Customer customer);

    @Named("adName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    AdDTO toDtoAdName(Ad ad);

    @Named("leadFormName")
    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "name")
    LeadFormDTO toDtoLeadFormName(LeadForm leadForm);
}
