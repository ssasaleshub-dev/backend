package com.salescrm.service.impl;

import com.salescrm.domain.*;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.repository.*;
import com.salescrm.service.LeadDetailedService;
import com.salescrm.service.dto.LeadDetailedDTO;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service implementation enriching Leads with their complete relational hierarchy.
 */
@Service
public class LeadDetailedServiceImpl implements LeadDetailedService {

    private final LeadRepository leadRepository;
    private final LeadPreferenceRepository preferenceRepository;
    private final AdSetRepository adSetRepository;
    private final CampaignRepository campaignRepository;

    public LeadDetailedServiceImpl(
        LeadRepository leadRepository,
        LeadPreferenceRepository preferenceRepository,
        AdSetRepository adSetRepository,
        CampaignRepository campaignRepository
    ) {
        this.leadRepository = leadRepository;
        this.preferenceRepository = preferenceRepository;
        this.adSetRepository = adSetRepository;
        this.campaignRepository = campaignRepository;
    }

    @Override
    public Flux<LeadDetailedDTO> findAllDetailed() {
        return leadRepository.findAll()
            .concatMap(this::enrichLead);
    }

    @Override
    public Mono<LeadDetailedDTO> findDetailedById(Long id) {
        return leadRepository.findById(id)
            .flatMap(this::enrichLead);
    }

    @Override
    @Transactional
    public Mono<LeadDetailedDTO> updateStatus(Long id, LeadStatus status, String notes) {
        return leadRepository.findById(id)
            .flatMap(lead -> {
                lead.setStatus(status);
                lead.setUpdatedAt(Instant.now());
                if (notes != null && !notes.isBlank()) {
                    lead.setNotes(notes);
                }
                return leadRepository.save(lead);
            })
            .flatMap(this::enrichLead);
    }

    private Mono<LeadDetailedDTO> enrichLead(Lead lead) {
        LeadDetailedDTO dto = new LeadDetailedDTO();
        dto.setId(lead.getId());
        dto.setExternalId(lead.getExternalId());
        dto.setCreatedTime(lead.getCreatedTime());
        dto.setIsOrganic(lead.getIsOrganic());
        dto.setPlatform(lead.getPlatform());
        dto.setStatus(lead.getStatus());
        dto.setNotes(lead.getNotes());
        dto.setCreatedAt(lead.getCreatedAt());

        if (lead.getCustomer() != null) {
            Customer c = lead.getCustomer();
            dto.setCustomerId(c.getId());
            dto.setCustomerName(c.getFullName());
            dto.setPhoneNumber(c.getPhoneNumber());
            dto.setLocation(c.getLocation());
        }

        if (lead.getLeadForm() != null) {
            LeadForm form = lead.getLeadForm();
            dto.setLeadFormId(form.getId());
            dto.setLeadFormName(form.getName());
        }

        Mono<Void> adHierarchyMono = Mono.empty();
        if (lead.getAd() != null) {
            Ad ad = lead.getAd();
            dto.setAdId(ad.getId());
            dto.setAdName(ad.getName());
            dto.setAdExternalId(ad.getExternalId());

            if (ad.getAdSetId() != null) {
                adHierarchyMono = adSetRepository.findById(ad.getAdSetId())
                    .flatMap(adSet -> {
                        dto.setAdSetId(adSet.getId());
                        dto.setAdSetName(adSet.getName());

                        if (adSet.getCampaignId() != null) {
                            return campaignRepository.findById(adSet.getCampaignId())
                                .doOnNext(camp -> {
                                    dto.setCampaignId(camp.getId());
                                    dto.setCampaignName(camp.getName());
                                    dto.setCampaignExternalId(camp.getExternalId());
                                })
                                .then();
                        }
                        return Mono.empty();
                    });
            }
        }

        Mono<Void> prefMono = preferenceRepository.findOneByLeadId(lead.getId())
            .doOnNext(p -> {
                dto.setPreferenceId(p.getId());
                dto.setMattressType(p.getMattressType());
                dto.setMattressSize(p.getMattressSize());
                dto.setBudgetRange(p.getBudgetRange());
                dto.setPurchaseTimeline(p.getPurchaseTimeline());
            })
            .then();

        return Mono.when(adHierarchyMono, prefMono).thenReturn(dto);
    }
}
