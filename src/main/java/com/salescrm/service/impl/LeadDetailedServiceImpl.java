package com.salescrm.service.impl;

import com.salescrm.domain.*;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.repository.*;
import com.salescrm.service.LeadDetailedService;
import com.salescrm.service.dto.LeadDetailedDTO;
import com.salescrm.security.AuthoritiesConstants;
import com.salescrm.security.SecurityUtils;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;
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
        return findAllDetailed(null);
    }

    @Override
    public Flux<LeadDetailedDTO> findAllDetailed(String visaTypeFilter) {
        return getAccessScope()
            .flatMapMany(scope -> leadRepository.findAll()
                .flatMap(this::enrichLead)
                .filter(lead -> scope.isLeadVisible(lead, visaTypeFilter))
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
                .filter(lead -> scope.isLeadVisible(lead, null))
            );
    }

    @Override
    @Transactional
    public Mono<LeadDetailedDTO> updateStatus(Long id, LeadStatus status, String notes) {
        return getAccessScope()
            .flatMap(scope -> leadRepository.findById(id)
                .flatMap(this::enrichLead)
                .flatMap(enriched -> {
                    if (!scope.isLeadVisible(enriched, null)) {
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
        return leadRepository.findById(id)
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
        return Mono.zip(
            SecurityUtils.getCurrentUserLogin().defaultIfEmpty(""),
            SecurityUtils.getCurrentUserAuthorities()
        ).map(tuple -> new LeadAccessScope(tuple.getT1(), tuple.getT2()));
    }

    /**
     * Determines visibility of leads based on user authorities.
     */
    private static class LeadAccessScope {
        final String login;
        final Set<String> authorities;
        final boolean isAdmin;
        final boolean isWorkVisa;
        final boolean isStudyVisa;
        final boolean isDomesticAdmission;

        LeadAccessScope(String login, Set<String> authorities) {
            this.login = login;
            this.authorities = authorities != null ? authorities : Collections.emptySet();
            this.isAdmin = this.authorities.contains(AuthoritiesConstants.ADMIN);
            this.isWorkVisa = this.authorities.contains(AuthoritiesConstants.WORK_VISA);
            this.isStudyVisa = this.authorities.contains(AuthoritiesConstants.STUDY_VISA);
            this.isDomesticAdmission = this.authorities.contains(AuthoritiesConstants.DOMESTIC_COLLEGE_ADMISSION);
        }

        boolean isLeadVisible(LeadDetailedDTO lead, String requestedVisaType) {
            // Admin has full visibility, respects optional query filter
            if (isAdmin) {
                if (requestedVisaType != null && !requestedVisaType.isBlank() && !"ALL".equalsIgnoreCase(requestedVisaType)) {
                    return matchesVisaType(lead.getVisaType(), requestedVisaType);
                }
                return true;
            }

            // Lead assigned directly to this user
            if (login != null && !login.isBlank() && login.equalsIgnoreCase(lead.getAssignedTo())) {
                return true;
            }

            // Staff 1: Work Visa
            if (isWorkVisa) {
                return matchesVisaType(lead.getVisaType(), "WORK_VISA");
            }

            // Staff 2: Study Visa
            if (isStudyVisa) {
                return matchesVisaType(lead.getVisaType(), "STUDY_VISA");
            }

            // Staff 3: Domestic College Admission
            if (isDomesticAdmission) {
                return matchesVisaType(lead.getVisaType(), "DOMESTIC_COLLEGE_ADMISSION");
            }

            // Fallback for unauthenticated dev calls
            if (requestedVisaType != null && !requestedVisaType.isBlank() && !"ALL".equalsIgnoreCase(requestedVisaType)) {
                return matchesVisaType(lead.getVisaType(), requestedVisaType);
            }
            return true;
        }

        private boolean matchesVisaType(String actual, String target) {
            if (actual == null || target == null) return false;
            String a = actual.trim().toUpperCase();
            String t = target.trim().toUpperCase();
            if (a.equals(t)) return true;
            if (t.contains("WORK") && a.contains("WORK")) return true;
            if (t.contains("STUDY") && (a.contains("STUDY") || a.contains("STUDENT"))) return true;
            if ((t.contains("DOMESTIC") || t.contains("COLLEGE") || t.contains("ADMISSION")) &&
                (a.contains("DOMESTIC") || a.contains("COLLEGE") || a.contains("ADMISSION"))) {
                return true;
            }
            return false;
        }
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
            dto.setCity(c.getLocation());
            dto.setEmail(c.getEmail());
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
                dto.setVisaType(p.getVisaType());
                dto.setAge(p.getAge());
                dto.setEducationalQualification(p.getEducationalQualification());
                dto.setAreYouInterested(p.getAreYouInterested());
                dto.setAreYouWorking(p.getAreYouWorking());
                dto.setLeadConfirm(p.getLeadConfirm());
                dto.setAssignedTo(p.getAssignedTo());

                // Legacy fallback
                dto.setMattressType(p.getMattressType());
                dto.setMattressSize(p.getMattressSize());
                dto.setBudgetRange(p.getBudgetRange());
                dto.setPurchaseTimeline(p.getPurchaseTimeline());
            })
            .then();

        return Mono.when(adHierarchyMono, prefMono).thenReturn(dto);
    }
}
