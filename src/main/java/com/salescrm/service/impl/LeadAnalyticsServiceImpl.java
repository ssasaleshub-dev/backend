package com.salescrm.service.impl;

import com.salescrm.service.LeadAnalyticsService;
import com.salescrm.service.LeadDetailedService;
import com.salescrm.service.dto.CrmAnalyticsDTO;
import com.salescrm.service.dto.LeadDetailedDTO;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Reactive service computing CRM intelligence and pipeline KPIs scoped to user roles.
 */
@Service
public class LeadAnalyticsServiceImpl implements LeadAnalyticsService {

    private final LeadDetailedService leadDetailedService;

    public LeadAnalyticsServiceImpl(LeadDetailedService leadDetailedService) {
        this.leadDetailedService = leadDetailedService;
    }

    @Override
    public Mono<CrmAnalyticsDTO> getDashboardAnalytics() {
        return getDashboardAnalytics(null);
    }

    @Override
    public Mono<CrmAnalyticsDTO> getDashboardAnalytics(String visaTypeFilter) {
        return leadDetailedService.findAllDetailed(visaTypeFilter)
            .collectList()
            .map(leads -> {
                CrmAnalyticsDTO dto = new CrmAnalyticsDTO();
                dto.setTotalLeads(leads.size());
                dto.setTotalCustomers(leads.stream().map(LeadDetailedDTO::getCustomerId).filter(Objects::nonNull).distinct().count());
                dto.setTotalCampaigns(leads.stream().map(LeadDetailedDTO::getCampaignId).filter(Objects::nonNull).distinct().count());
                dto.setTotalAds(leads.stream().map(LeadDetailedDTO::getAdId).filter(Objects::nonNull).distinct().count());

                dto.setLeadsByStatus(leads.stream()
                    .filter(l -> l.getStatus() != null)
                    .collect(Collectors.groupingBy(l -> l.getStatus().name(), Collectors.counting())));

                dto.setLeadsByPlatform(leads.stream()
                    .filter(l -> l.getPlatform() != null)
                    .collect(Collectors.groupingBy(l -> l.getPlatform().name(), Collectors.counting())));

                dto.setLeadsByLocation(leads.stream()
                    .filter(l -> l.getLocation() != null && !l.getLocation().isBlank())
                    .collect(Collectors.groupingBy(l -> l.getLocation().toLowerCase().trim(), Collectors.counting())));

                dto.setLeadsByVisaType(leads.stream()
                    .filter(l -> l.getVisaType() != null && !l.getVisaType().isBlank())
                    .collect(Collectors.groupingBy(LeadDetailedDTO::getVisaType, Collectors.counting())));

                dto.setLeadsByAssignedTo(leads.stream()
                    .filter(l -> l.getAssignedTo() != null && !l.getAssignedTo().isBlank())
                    .collect(Collectors.groupingBy(LeadDetailedDTO::getAssignedTo, Collectors.counting())));

                dto.setLeadsByMattressSize(leads.stream()
                    .filter(l -> l.getMattressSize() != null && !l.getMattressSize().isBlank())
                    .collect(Collectors.groupingBy(l -> l.getMattressSize().toLowerCase().trim(), Collectors.counting())));

                dto.setLeadsByBudget(leads.stream()
                    .filter(l -> l.getBudgetRange() != null && !l.getBudgetRange().isBlank())
                    .collect(Collectors.groupingBy(LeadDetailedDTO::getBudgetRange, Collectors.counting())));

                return dto;
            });
    }
}

