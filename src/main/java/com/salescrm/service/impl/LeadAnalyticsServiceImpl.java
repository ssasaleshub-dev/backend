package com.salescrm.service.impl;

import com.salescrm.repository.*;
import com.salescrm.service.LeadAnalyticsService;
import com.salescrm.service.dto.CrmAnalyticsDTO;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Reactive service computing CRM intelligence and pipeline KPIs.
 */
@Service
public class LeadAnalyticsServiceImpl implements LeadAnalyticsService {

    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final CampaignRepository campaignRepository;
    private final AdRepository adRepository;
    private final LeadPreferenceRepository preferenceRepository;

    public LeadAnalyticsServiceImpl(
        LeadRepository leadRepository,
        CustomerRepository customerRepository,
        CampaignRepository campaignRepository,
        AdRepository adRepository,
        LeadPreferenceRepository preferenceRepository
    ) {
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.campaignRepository = campaignRepository;
        this.adRepository = adRepository;
        this.preferenceRepository = preferenceRepository;
    }

    @Override
    public Mono<CrmAnalyticsDTO> getDashboardAnalytics() {
        CrmAnalyticsDTO dto = new CrmAnalyticsDTO();

        Mono<Long> leadsCount = leadRepository.count();
        Mono<Long> customersCount = customerRepository.count();
        Mono<Long> campaignsCount = campaignRepository.count();
        Mono<Long> adsCount = adRepository.count();

        Mono<Map<String, Long>> statusGroup = leadRepository.findAll()
            .collectList()
            .map(leads -> leads.stream()
                .filter(l -> l.getStatus() != null)
                .collect(Collectors.groupingBy(l -> l.getStatus().name(), Collectors.counting())));

        Mono<Map<String, Long>> platformGroup = leadRepository.findAll()
            .collectList()
            .map(leads -> leads.stream()
                .filter(l -> l.getPlatform() != null)
                .collect(Collectors.groupingBy(l -> l.getPlatform().name(), Collectors.counting())));

        Mono<Map<String, Long>> locationGroup = customerRepository.findAll()
            .collectList()
            .map(customers -> customers.stream()
                .filter(c -> c.getLocation() != null && !c.getLocation().isBlank())
                .collect(Collectors.groupingBy(c -> c.getLocation().toLowerCase().trim(), Collectors.counting())));

        Mono<Map<String, Long>> sizeGroup = preferenceRepository.findAll()
            .collectList()
            .map(prefs -> prefs.stream()
                .filter(p -> p.getMattressSize() != null && !p.getMattressSize().isBlank())
                .collect(Collectors.groupingBy(p -> p.getMattressSize().toLowerCase().trim(), Collectors.counting())));

        Mono<Map<String, Long>> budgetGroup = preferenceRepository.findAll()
            .collectList()
            .map(prefs -> prefs.stream()
                .filter(p -> p.getBudgetRange() != null && !p.getBudgetRange().isBlank())
                .collect(Collectors.groupingBy(p -> p.getBudgetRange().trim(), Collectors.counting())));

        return Mono.zip(leadsCount, customersCount, campaignsCount, adsCount)
            .flatMap(counts -> {
                dto.setTotalLeads(counts.getT1());
                dto.setTotalCustomers(counts.getT2());
                dto.setTotalCampaigns(counts.getT3());
                dto.setTotalAds(counts.getT4());

                return Mono.zip(statusGroup, platformGroup, locationGroup, sizeGroup, budgetGroup)
                    .map(groups -> {
                        dto.setLeadsByStatus(groups.getT1());
                        dto.setLeadsByPlatform(groups.getT2());
                        dto.setLeadsByLocation(groups.getT3());
                        dto.setLeadsByMattressSize(groups.getT4());
                        dto.setLeadsByBudget(groups.getT5());
                        return dto;
                    });
            });
    }
}
