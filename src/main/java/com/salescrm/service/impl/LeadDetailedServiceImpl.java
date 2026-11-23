package com.salescrm.service.impl;

import com.salescrm.domain.*;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.repository.*;
import com.salescrm.security.LeadAccessScope;
import com.salescrm.service.LeadAccessService;
import com.salescrm.service.LeadDetailedService;
import com.salescrm.service.dto.LeadDetailedDTO;
import java.time.Instant;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service implementation enriching Leads with their complete relational hierarchy
 * and enforcing role-based visibility for staff (Work Visa, Study Visa, Domestic College Admission).
 */
@Service
public class LeadDetailedServiceImpl implements LeadDetailedService {

    private final LeadRepository leadRepository;
    private final LeadPreferenceRepository preferenceRepository;
    private final AdSetRepository adSetRepository;
    private final CampaignRepository campaignRepository;
    private final LeadAccessService leadAccessService;

    public LeadDetailedServiceImpl(
        LeadRepository leadRepository,
        LeadPreferenceRepository preferenceRepository,
        AdSetRepository adSetRepository,
        CampaignRepository campaignRepository,
        LeadAccessService leadAccessService
    ) {
        this.leadRepository = leadRepository;
        this.preferenceRepository = preferenceRepository;
        this.adSetRepository = adSetRepository;
        this.campaignRepository = campaignRepository;
        this.leadAccessService = leadAccessService;
    }

    @Override
    public Flux<LeadDetailedDTO> findAllDetailed() {
        return findAllDetailed(null);
    }

    @Override
    public Flux<LeadDetailedDTO> findAllDetailed(String visaTypeFilter) {
        return getAccessScope()
            .flatMapMany(scope -> leadRepository.findAll()
                .flatMap(this::enrichLead)
                .filter(lead -> scope.isVisible(lead.getVisaType(), lead.getAssignedTo(), visaTypeFilter))
                .sort((a, b) -> {
                    if (a.getCreatedTime() == null && b.getCreatedTime() == null) return 0;
                    if (a.getCreatedTime() == null) return 1;
                    if (b.getCreatedTime() == null) return -1;
                    return b.getCreatedTime().compareTo(a.getCreatedTime());
                })
            );
    }

    @Override
    public Mono<LeadDetailedDTO> findDetailedById(Long id) {
        return getAccessScope()
            .flatMap(scope -> leadRepository.findById(id)
                .flatMap(this::enrichLead)
                .filter(lead -> scope.canAccess(lead.getVisaType(), lead.getAssignedTo()))
            );
    }

    @Override
    @Transactional
    public Mono<LeadDetailedDTO> updateStatus(Long id, LeadStatus status, String notes) {
        return getAccessScope()
            .flatMap(scope -> leadRepository.findById(id)
                .flatMap(this::enrichLead)
                .flatMap(enriched -> {
                    if (!scope.canAccess(enriched.getVisaType(), enriched.getAssignedTo())) {
                        return Mono.error(new AccessDeniedException("Unauthorized to modify this lead"));
                    }
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
                })
            );
    }

    @Override
    @Transactional
    public Mono<LeadDetailedDTO> updateAssignment(Long id, String assignedTo) {
        // Only administrators may (re)assign leads to staff
        return leadAccessService.requireAdmin().then(leadRepository.findById(id))
            .flatMap(lead -> preferenceRepository.findOneByLeadId(id)
                .switchIfEmpty(
                    Mono.defer(() -> {
                        LeadPreference newPref = new LeadPreference();
                        newPref.setLeadId(id);
                        newPref.setCreatedAt(Instant.now());
                        return Mono.just(newPref);
                    })
                )
                .flatMap(pref -> {
                    pref.setAssignedTo(assignedTo);
                    return preferenceRepository.save(pref);
                })
                .then(findDetailedById(id))
            );
    }

    private Mono<LeadAccessScope> getAccessScope() {
        return leadAccessService.currentScope();
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
        dto.setMetaPageId(lead.getMetaPageId());
        dto.setMetaFormResponses(lead.getMetaFormResponses());

        if (lead.getCustomer() != null) {
            Customer c = lead.getCustomer();
            dto.setCustomerId(c.getId());
            dto.setCustomerName(c.getFullName());
            dto.setPhoneNumber(c.getPhoneNumber());
            dto.setLocation(c.getLocation());
            dto.setCity(c.getLocation());
            dto.setEmail(c.getEmail());
        }

        if (lead.getLeadForm() != null) {
            LeadForm form = lead.getLeadForm();
            dto.setLeadFormId(form.getId());
            dto.setLeadFormName(form.getName());
            dto.setLeadFormExternalId(form.getExternalId());
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
                        dto.setAdSetExternalId(adSet.getExternalId());

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
                dto.setVisaType(p.getVisaType());
                dto.setAge(p.getAge());
                dto.setEducationalQualification(p.getEducationalQualification());
                dto.setAreYouInterested(p.getAreYouInterested());
                dto.setAreYouWorking(p.getAreYouWorking());
                dto.setLeadConfirm(p.getLeadConfirm());
                dto.setAssignedTo(p.getAssignedTo());

                // Financial settlement profile
                String rawBudget = p.getBudgetRange();
                boolean isScore = rawBudget != null && (
                    rawBudget.contains("%") ||
                    rawBudget.toLowerCase().contains("cgpa") ||
                    rawBudget.toLowerCase().contains("gpa")
                );

                if (isScore) {
                    // Legacy record stored CGPA inside budget_range; rescue it into score
                    String cleanedScore = rawBudget.replace("_", " ").trim();
                    dto.setStudentPercentage(cleanedScore);
                    dto.setCgpa(cleanedScore);
                    dto.setBudgetRange(null);
                    dto.setClientBudget(null);
                } else {
                    dto.setBudgetRange(rawBudget);
                    dto.setClientBudget(rawBudget);
                }

                String pStatus = p.getPaymentStatus() != null && !p.getPaymentStatus().isBlank() ? p.getPaymentStatus() : "UNPAID";
                dto.setPaymentStatus(pStatus);
                dto.setPaymentDetails(p.getPaymentDetails());

                dto.setPurchaseTimeline(p.getPurchaseTimeline());
                dto.setClosingDate(p.getPurchaseTimeline());
            })
            .then();

        // Extract student percentage / CGPA from candidate notes if present
        if (lead.getNotes() != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(?i)(?:what_is_your_cgpa_or_current_percentage|cgpa[\\s/]*percentage|cgpa|percentage|score|marks)\\s*[:=?/]\\s*([^|\\n,;]+)"
            ).matcher(lead.getNotes());
            if (m.find()) {
                String score = m.group(1).trim().replace("_", " ");
                dto.setStudentPercentage(score);
                dto.setCgpa(score);
            }
        }

        return Mono.when(adHierarchyMono, prefMono).thenReturn(dto);
    }
}
