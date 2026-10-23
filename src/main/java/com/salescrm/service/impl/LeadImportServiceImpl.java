package com.salescrm.service.impl;

import com.salescrm.domain.*;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import com.salescrm.repository.*;
import com.salescrm.service.LeadImportService;
import com.salescrm.service.dto.ExcelLeadRowDTO;
import com.salescrm.service.dto.LeadImportResultDTO;
import com.salescrm.service.ingestion.IngestionStrategyFactory;
import com.salescrm.service.ingestion.LeadIngestionStrategy;
import com.salescrm.service.util.DateTimeParser;
import com.salescrm.service.util.PhoneNormalizer;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Production implementation of LeadImportService orchestrating
 * the reactive normalization and persistence pipeline.
 *
 * Adheres to SOLID Principles:
 * - Single Responsibility: Orchestration of domain ingestion workflow
 * - Dependency Inversion: Injects repository and utility abstractions
 */
@Service
public class LeadImportServiceImpl implements LeadImportService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadImportServiceImpl.class);

    private final IngestionStrategyFactory strategyFactory;
    private final CustomerRepository customerRepository;
    private final CampaignRepository campaignRepository;
    private final AdSetRepository adSetRepository;
    private final AdRepository adRepository;
    private final LeadFormRepository leadFormRepository;
    private final LeadRepository leadRepository;
    private final LeadPreferenceRepository leadPreferenceRepository;
    private final PhoneNormalizer phoneNormalizer;
    private final DateTimeParser dateTimeParser;

    public LeadImportServiceImpl(
        IngestionStrategyFactory strategyFactory,
        CustomerRepository customerRepository,
        CampaignRepository campaignRepository,
        AdSetRepository adSetRepository,
        AdRepository adRepository,
        LeadFormRepository leadFormRepository,
        LeadRepository leadRepository,
        LeadPreferenceRepository leadPreferenceRepository,
        PhoneNormalizer phoneNormalizer,
        DateTimeParser dateTimeParser
    ) {
        this.strategyFactory = strategyFactory;
        this.customerRepository = customerRepository;
        this.campaignRepository = campaignRepository;
        this.adSetRepository = adSetRepository;
        this.adRepository = adRepository;
        this.leadFormRepository = leadFormRepository;
        this.leadRepository = leadRepository;
        this.leadPreferenceRepository = leadPreferenceRepository;
        this.phoneNormalizer = phoneNormalizer;
        this.dateTimeParser = dateTimeParser;
    }

    @Override
    public Mono<LeadImportResultDTO> importFile(String fileName, String contentType, InputStream inputStream) {
        long startTime = System.currentTimeMillis();
        LeadImportResultDTO result = LeadImportResultDTO.create(fileName);

        return Mono.fromCallable(() -> {
            LeadIngestionStrategy strategy = strategyFactory.getStrategy(fileName, contentType);
            return strategy.parse(inputStream);
        })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(rows -> importRows(fileName, rows))
            .map(importResult -> {
                importResult.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                return importResult;
            })
            .onErrorResume(ex -> {
                LOG.error("Failed to parse file '{}': {}", fileName, ex.getMessage(), ex);
                result.addError("File parsing failure: " + ex.getMessage());
                result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                return Mono.just(result);
            });
    }

    @Override
    public Mono<LeadImportResultDTO> importRows(String sourceName, List<ExcelLeadRowDTO> rows) {
        long startTime = System.currentTimeMillis();
        LeadImportResultDTO result = LeadImportResultDTO.create(sourceName);
        result.setTotalRowsProcessed(rows.size());

        if (rows.isEmpty()) {
            result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
            return Mono.just(result);
        }

        return Flux.fromIterable(rows)
            .concatMap(this::processSingleRow)
            .collectList()
            .map(processedList -> {
                for (RowProcessResult item : processedList) {
                    if (item.success) {
                        result.addSuccess(item.leadExternalId);
                    } else {
                        result.addError(item.errorMessage);
                    }
                }
                result.setExecutionTimeMs(System.currentTimeMillis() - startTime);
                LOG.info("Lead ingestion completed: {} succeeded, {} failed out of {}",
                    result.getSuccessfulRows(), result.getFailedRows(), result.getTotalRowsProcessed());
                return result;
            });
    }

    @Override
    public Mono<LeadImportResultDTO> importSampleData() {
        return Mono.defer(() -> {
            try {
                ClassPathResource xlsxResource = new ClassPathResource("data/sample_leads.xlsx");
                if (xlsxResource.exists()) {
                    return importFile(
                        "sample_leads.xlsx",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        xlsxResource.getInputStream()
                    );
                }

                ClassPathResource csvResource = new ClassPathResource("data/leads_data.csv");
                if (csvResource.exists()) {
                    return importFile("sample_leads.csv", "text/csv", csvResource.getInputStream());
                }

                ClassPathResource tsvResource = new ClassPathResource("data/leads_data.tsv");
                if (tsvResource.exists()) {
                    return importFile("sample_leads.tsv", "text/tab-separated-values", tsvResource.getInputStream());
                }

                return Mono.error(new IllegalStateException("Sample seed file not found in classpath"));
            } catch (Exception e) {
                return Mono.error(e);
            }
        });
    }

    /**
     * Processes a single row transactionally, resolving customer, marketing entities,
     * lead event, and product preferences.
     */
    @Transactional
    public Mono<RowProcessResult> processSingleRow(ExcelLeadRowDTO row) {
        Instant createdTime = dateTimeParser.parse(row.getCreatedTime());
        String normalizedPhone = phoneNormalizer.normalize(row.getPhoneNumber());

        if (row.getId() == null || row.getId().isBlank()) {
            return Mono.just(new RowProcessResult(false, null, "Row " + row.getRowNumber() + ": Missing Lead ID"));
        }

        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            return Mono.just(new RowProcessResult(false, null, "Row " + row.getRowNumber() + ": Missing Phone Number"));
        }

        return resolveCustomer(row, normalizedPhone, createdTime)
            .flatMap(customer ->
                resolveCampaign(row, createdTime)
                    .flatMap(campaign -> resolveAdSet(row, createdTime, campaign))
                    .flatMap(adSet -> resolveAd(row, createdTime, adSet))
                    .flatMap(ad ->
                        resolveLeadForm(row, createdTime)
                            .flatMap(leadForm -> saveOrUpdateLead(row, createdTime, customer, ad, leadForm))
                    )
            )
            .flatMap(lead -> saveOrUpdatePreference(row, lead))
            .map(lead -> new RowProcessResult(true, lead.getExternalId(), null))
            .onErrorResume(ex -> {
                LOG.error("Failed to process row {}: {}", row.getRowNumber(), ex.getMessage(), ex);
                return Mono.just(new RowProcessResult(false, row.getId(), "Row " + row.getRowNumber() + ": " + ex.getMessage()));
            });
    }

    private Mono<Customer> resolveCustomer(ExcelLeadRowDTO row, String normalizedPhone, Instant createdTime) {
        String normalizedEmail = (row.getEmail() != null && !row.getEmail().isBlank())
            ? row.getEmail().trim().toLowerCase()
            : null;

        Mono<Customer> customerLookup = customerRepository.findByPhoneNumber(normalizedPhone);
        if (normalizedEmail != null) {
            customerLookup = customerLookup.switchIfEmpty(customerRepository.findByEmail(normalizedEmail));
        }

        return customerLookup
            .flatMap(existing -> {
                boolean changed = false;
                if ((existing.getFullName() == null || existing.getFullName().isBlank() || "Customer".equalsIgnoreCase(existing.getFullName()))
                    && row.getFullName() != null && !row.getFullName().isBlank()) {
                    existing.setFullName(row.getFullName().trim());
                    changed = true;
                }
                if ((existing.getLocation() == null || existing.getLocation().isBlank())
                    && row.getLocation() != null && !row.getLocation().isBlank()) {
                    existing.setLocation(row.getLocation().trim());
                    changed = true;
                }
                if ((existing.getEmail() == null || existing.getEmail().isBlank()) && normalizedEmail != null) {
                    existing.setEmail(normalizedEmail);
                    changed = true;
                }
                return changed ? customerRepository.save(existing) : Mono.just(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
                Customer newCustomer = new Customer();
                String name = (row.getFullName() != null && !row.getFullName().isBlank())
                    ? row.getFullName().trim()
                    : "Customer";
                newCustomer.setFullName(name);
                newCustomer.setPhoneNumber(normalizedPhone);
                newCustomer.setRawPhoneNumber(row.getPhoneNumber());
                newCustomer.setLocation(row.getLocation() != null ? row.getLocation().trim() : null);
                newCustomer.setEmail(normalizedEmail);
                newCustomer.setCreatedAt(createdTime != null ? createdTime : Instant.now());
                return customerRepository.save(newCustomer);
            }));
    }

    private Mono<Campaign> resolveCampaign(ExcelLeadRowDTO row, Instant createdTime) {
        String extId = row.getCampaignId();
        if (extId == null || extId.isBlank()) {
            return Mono.just(new Campaign()); // Sentinel empty
        }

        return campaignRepository.findByExternalId(extId)
            .switchIfEmpty(Mono.defer(() -> {
                Campaign campaign = new Campaign();
                campaign.setExternalId(extId);
                String name = (row.getCampaignName() != null && !row.getCampaignName().isBlank())
                    ? row.getCampaignName().trim()
                    : "Campaign " + extId;
                campaign.setName(name);
                campaign.setStatus("ACTIVE");
                campaign.setCreatedAt(createdTime != null ? createdTime : Instant.now());
                campaign.setUpdatedAt(createdTime != null ? createdTime : Instant.now());
                return campaignRepository.save(campaign);
            }));
    }

    private Mono<AdSet> resolveAdSet(ExcelLeadRowDTO row, Instant createdTime, Campaign campaign) {
        String extId = row.getAdsetId();
        if (extId == null || extId.isBlank()) {
            return Mono.just(new AdSet());
        }

        return adSetRepository.findByExternalId(extId)
            .flatMap(existing -> {
                if (existing.getCampaignId() == null && campaign != null && campaign.getId() != null) {
                    existing.setCampaign(campaign);
                    return adSetRepository.save(existing);
                }
                return Mono.just(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
                AdSet adSet = new AdSet();
                adSet.setExternalId(extId);
                String name = (row.getAdsetName() != null && !row.getAdsetName().isBlank())
                    ? row.getAdsetName().trim()
                    : "AdSet " + extId;
                adSet.setName(name);
                adSet.setStatus("ACTIVE");
                adSet.setCreatedAt(createdTime != null ? createdTime : Instant.now());
                if (campaign != null && campaign.getId() != null) {
                    adSet.setCampaign(campaign);
                }
                return adSetRepository.save(adSet);
            }));
    }

    private Mono<Ad> resolveAd(ExcelLeadRowDTO row, Instant createdTime, AdSet adSet) {
        String extId = row.getAdId();
        if (extId == null || extId.isBlank()) {
            return Mono.just(new Ad());
        }

        return adRepository.findByExternalId(extId)
            .flatMap(existing -> {
                if (existing.getAdSetId() == null && adSet != null && adSet.getId() != null) {
                    existing.setAdSet(adSet);
                    return adRepository.save(existing);
                }
                return Mono.just(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
                Ad ad = new Ad();
                ad.setExternalId(extId);
                String name = (row.getAdName() != null && !row.getAdName().isBlank())
                    ? row.getAdName().trim()
                    : "Ad " + extId;
                ad.setName(name);
                ad.setStatus("ACTIVE");
                ad.setCreatedAt(createdTime != null ? createdTime : Instant.now());
                if (adSet != null && adSet.getId() != null) {
                    ad.setAdSet(adSet);
                }
                return adRepository.save(ad);
            }));
    }

    private Mono<LeadForm> resolveLeadForm(ExcelLeadRowDTO row, Instant createdTime) {
        String extId = row.getFormId();
        if (extId == null || extId.isBlank()) {
            return Mono.just(new LeadForm());
        }

        return leadFormRepository.findByExternalId(extId)
            .switchIfEmpty(Mono.defer(() -> {
                LeadForm form = new LeadForm();
                form.setExternalId(extId);
                String name = (row.getFormName() != null && !row.getFormName().isBlank())
                    ? row.getFormName().trim()
                    : "Form " + extId;
                form.setName(name);
                form.setStatus("ACTIVE");
                form.setCreatedAt(createdTime != null ? createdTime : Instant.now());
                return leadFormRepository.save(form);
            }));
    }

    private Mono<Lead> saveOrUpdateLead(
        ExcelLeadRowDTO row,
        Instant createdTime,
        Customer customer,
        Ad ad,
        LeadForm leadForm
    ) {
        PlatformType platform = parsePlatform(row.getPlatform());
        LeadStatus status = parseStatus(row.getLeadStatus());
        boolean isOrganic = parseOrganic(row.getIsOrganic());

        String campaignDesc = (row.getCampaignName() != null && !row.getCampaignName().isBlank())
            ? row.getCampaignName().trim()
            : "Meta Ads";
        StringBuilder notesBuilder = new StringBuilder("Imported from ").append(campaignDesc);
        if (row.getCgpaOrPercentage() != null && !row.getCgpaOrPercentage().isBlank()) {
            notesBuilder.append(" | CGPA/Percentage: ").append(row.getCgpaOrPercentage().trim());
        }
        String eduQual = mapEducationalQualification(
            row.getCurrentStatus() != null && !row.getCurrentStatus().isBlank()
                ? row.getCurrentStatus()
                : row.getEducationalQualification()
        );
        if (eduQual != null && !eduQual.isBlank()) {
            notesBuilder.append(" | Qualification: ").append(eduQual);
        }

        final String formattedNotes = notesBuilder.toString();

        return leadRepository.findByExternalId(row.getId())
            .flatMap(existing -> {
                existing.setStatus(status);
                existing.setUpdatedAt(Instant.now());
                if (customer != null && customer.getId() != null && existing.getCustomerId() == null) {
                    existing.setCustomer(customer);
                }
                if (ad != null && ad.getId() != null && existing.getAdId() == null) {
                    existing.setAd(ad);
                }
                if (leadForm != null && leadForm.getId() != null && existing.getLeadFormId() == null) {
                    existing.setLeadForm(leadForm);
                }
                if (row.getCgpaOrPercentage() != null && !row.getCgpaOrPercentage().isBlank()) {
                    existing.setNotes(formattedNotes);
                }
                return leadRepository.save(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
                Lead newLead = new Lead();
                newLead.setExternalId(row.getId());
                newLead.setCreatedTime(createdTime != null ? createdTime : Instant.now());
                newLead.setIsOrganic(isOrganic);
                newLead.setPlatform(platform);
                newLead.setStatus(status);
                newLead.setNotes(formattedNotes);
                newLead.setCreatedAt(createdTime != null ? createdTime : Instant.now());
                newLead.setUpdatedAt(createdTime != null ? createdTime : Instant.now());

                if (customer != null && customer.getId() != null) {
                    newLead.setCustomer(customer);
                }
                if (ad != null && ad.getId() != null) {
                    newLead.setAd(ad);
                }
                if (leadForm != null && leadForm.getId() != null) {
                    newLead.setLeadForm(leadForm);
                }

                return leadRepository.save(newLead);
            }));
    }

    private Mono<Lead> saveOrUpdatePreference(ExcelLeadRowDTO row, Lead lead) {
        if (lead == null || lead.getId() == null) {
            return Mono.justOrEmpty(lead);
        }

        String eduQual = mapEducationalQualification(
            row.getCurrentStatus() != null && !row.getCurrentStatus().isBlank()
                ? row.getCurrentStatus()
                : row.getEducationalQualification()
        );

        return leadPreferenceRepository.findOneByLeadId(lead.getId())
            .flatMap(existing -> {
                if (row.getVisaType() != null && !row.getVisaType().isBlank()) existing.setVisaType(row.getVisaType());
                if (row.getAge() != null && !row.getAge().isBlank()) existing.setAge(row.getAge());
                if (eduQual != null && !eduQual.isBlank()) existing.setEducationalQualification(eduQual);
                if (row.getAreYouInterested() != null && !row.getAreYouInterested().isBlank()) existing.setAreYouInterested(row.getAreYouInterested());
                if (row.getAreYouWorking() != null && !row.getAreYouWorking().isBlank()) existing.setAreYouWorking(row.getAreYouWorking());
                if (row.getLeadConfirm() != null && !row.getLeadConfirm().isBlank()) existing.setLeadConfirm(row.getLeadConfirm());

                // Store CGPA or budget
                if (row.getCgpaOrPercentage() != null && !row.getCgpaOrPercentage().isBlank()) {
                    existing.setBudgetRange(row.getCgpaOrPercentage().trim());
                }

                // Legacy fields
                if (row.getMattressType() != null && !row.getMattressType().isBlank()) existing.setMattressType(row.getMattressType());
                if (row.getMattressSize() != null && !row.getMattressSize().isBlank()) existing.setMattressSize(row.getMattressSize());
                if (row.getPreferredBudget() != null && !row.getPreferredBudget().isBlank()) existing.setBudgetRange(row.getPreferredBudget());
                if (row.getPlanningToBuy() != null && !row.getPlanningToBuy().isBlank()) existing.setPurchaseTimeline(row.getPlanningToBuy());
                return leadPreferenceRepository.save(existing).thenReturn(lead);
            })
            .switchIfEmpty(Mono.defer(() -> {
                LeadPreference pref = new LeadPreference();
                pref.setLead(lead);
                pref.setVisaType(row.getVisaType());
                pref.setAge(row.getAge());
                pref.setEducationalQualification(eduQual);
                pref.setAreYouInterested(row.getAreYouInterested());
                pref.setAreYouWorking(row.getAreYouWorking());
                pref.setLeadConfirm(row.getLeadConfirm());

                // Store CGPA or budget
                if (row.getCgpaOrPercentage() != null && !row.getCgpaOrPercentage().isBlank()) {
                    pref.setBudgetRange(row.getCgpaOrPercentage().trim());
                }

                // Legacy fields
                pref.setMattressType(row.getMattressType());
                pref.setMattressSize(row.getMattressSize());
                if (row.getPreferredBudget() != null && !row.getPreferredBudget().isBlank()) {
                    pref.setBudgetRange(row.getPreferredBudget());
                }
                pref.setPurchaseTimeline(row.getPlanningToBuy());
                pref.setCreatedAt(lead.getCreatedTime() != null ? lead.getCreatedTime() : Instant.now());
                return leadPreferenceRepository.save(pref).thenReturn(lead);
            }));
    }

    private String mapEducationalQualification(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String lower = raw.trim().toLowerCase();
        String norm = lower.replace("-", " ").replace("_", " ");
        if (norm.contains("completed ug") || norm.contains("bachelor") || norm.contains("graduate")) {
            return "Bachelor Degree / UG Graduate";
        }
        if (norm.contains("final year") || norm.contains("finallyear")) {
            return "Final Year UG Student";
        }
        if (norm.contains("other")) {
            return "Other";
        }
        return raw.trim();
    }

    private PlatformType parsePlatform(String raw) {
        if (raw == null || raw.isBlank()) {
            return PlatformType.IG;
        }
        String p = raw.trim().toLowerCase();
        if (p.contains("fb") || p.contains("facebook")) {
            return PlatformType.FB;
        }
        if (p.contains("ig") || p.contains("instagram")) {
            return PlatformType.IG;
        }
        if (p.contains("whatsapp") || p.contains("wa")) {
            return PlatformType.WHATSAPP;
        }
        return PlatformType.IG;
    }

    private LeadStatus parseStatus(String raw) {
        if (raw == null || raw.isBlank()) {
            return LeadStatus.CREATED;
        }
        String r = raw.trim().toLowerCase();
        if (r.contains("proposal") || r.contains("doc")) {
            return LeadStatus.PROPOSAL_SENT;
        }
        if (r.contains("negotiat") || r.contains("process")) {
            return LeadStatus.NEGOTIATION;
        }
        if (r.contains("won") || r.contains("approved") || r.contains("enrolled")) {
            return LeadStatus.WON;
        }
        if (r.contains("complete") || r.contains("created") || r.contains("new")) {
            return LeadStatus.CREATED;
        }
        if (r.contains("contact")) {
            return LeadStatus.CONTACTED;
        }
        if (r.contains("qualif")) {
            return LeadStatus.QUALIFIED;
        }
        if (r.contains("convert")) {
            return LeadStatus.CONVERTED;
        }
        if (r.contains("lost")) {
            return LeadStatus.LOST;
        }
        try {
            return LeadStatus.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return LeadStatus.CREATED;
        }
    }

    private boolean parseOrganic(String raw) {
        if (raw == null) {
            return false;
        }
        String r = raw.trim().toLowerCase();
        return r.equals("true") || r.equals("1") || r.equals("yes");
    }

    private static class RowProcessResult {
        final boolean success;
        final String leadExternalId;
        final String errorMessage;

        RowProcessResult(boolean success, String leadExternalId, String errorMessage) {
            this.success = success;
            this.leadExternalId = leadExternalId;
            this.errorMessage = errorMessage;
        }
    }
}
