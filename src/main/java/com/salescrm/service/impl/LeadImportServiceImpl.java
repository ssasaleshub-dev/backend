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
        return customerRepository.findByPhoneNumber(normalizedPhone)
            .flatMap(existing -> {
                // Update customer info if name or location wasn't set
                boolean changed = false;
                if ((existing.getFullName() == null || existing.getFullName().isBlank()) && row.getFullName() != null) {
                    existing.setFullName(row.getFullName());
                    changed = true;
                }
                if ((existing.getLocation() == null || existing.getLocation().isBlank()) && row.getLocation() != null) {
                    existing.setLocation(row.getLocation());
                    changed = true;
                }
                return changed ? customerRepository.save(existing) : Mono.just(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
                Customer newCustomer = new Customer();
                newCustomer.setFullName(row.getFullName() != null ? row.getFullName() : "Customer");
                newCustomer.setPhoneNumber(normalizedPhone);
                newCustomer.setRawPhoneNumber(row.getPhoneNumber());
                newCustomer.setLocation(row.getLocation());
                newCustomer.setCreatedAt(createdTime);
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
                campaign.setName(row.getCampaignName() != null ? row.getCampaignName() : "Campaign " + extId);
                campaign.setStatus("ACTIVE");
                campaign.setCreatedAt(createdTime);
                campaign.setUpdatedAt(createdTime);
                return campaignRepository.save(campaign);
            }));
    }

    private Mono<AdSet> resolveAdSet(ExcelLeadRowDTO row, Instant createdTime, Campaign campaign) {
        String extId = row.getAdsetId();
        if (extId == null || extId.isBlank()) {
            return Mono.just(new AdSet());
        }

        return adSetRepository.findByExternalId(extId)
            .switchIfEmpty(Mono.defer(() -> {
                AdSet adSet = new AdSet();
                adSet.setExternalId(extId);
                adSet.setName(row.getAdsetName() != null ? row.getAdsetName() : "AdSet " + extId);
                adSet.setStatus("ACTIVE");
                adSet.setCreatedAt(createdTime);
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
            .switchIfEmpty(Mono.defer(() -> {
                Ad ad = new Ad();
                ad.setExternalId(extId);
                ad.setName(row.getAdName() != null ? row.getAdName() : "Ad " + extId);
                ad.setStatus("ACTIVE");
                ad.setCreatedAt(createdTime);
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
                form.setName(row.getFormName() != null ? row.getFormName() : "Form " + extId);
                form.setStatus("ACTIVE");
                form.setCreatedAt(createdTime);
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

        return leadRepository.findByExternalId(row.getId())
            .flatMap(existing -> {
                existing.setStatus(status);
                existing.setUpdatedAt(Instant.now());
                if (row.getLeadStatus() != null) {
                    existing.setNotes("Updated via Excel import: " + row.getLeadStatus());
                }
                return leadRepository.save(existing);
            })
            .switchIfEmpty(Mono.defer(() -> {
                Lead newLead = new Lead();
                newLead.setExternalId(row.getId());
                newLead.setCreatedTime(createdTime);
                newLead.setIsOrganic(isOrganic);
                newLead.setPlatform(platform);
                newLead.setStatus(status);
                newLead.setNotes("Imported from " + (row.getCampaignName() != null ? row.getCampaignName() : "Excel"));
                newLead.setCreatedAt(createdTime);
                newLead.setUpdatedAt(createdTime);

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

        return leadPreferenceRepository.findOneByLeadId(lead.getId())
            .flatMap(existing -> {
                existing.setMattressType(row.getMattressType());
                existing.setMattressSize(row.getMattressSize());
                existing.setBudgetRange(row.getPreferredBudget());
                existing.setPurchaseTimeline(row.getPlanningToBuy());
                return leadPreferenceRepository.save(existing).thenReturn(lead);
            })
            .switchIfEmpty(Mono.defer(() -> {
                LeadPreference pref = new LeadPreference();
                pref.setLead(lead);
                pref.setMattressType(row.getMattressType());
                pref.setMattressSize(row.getMattressSize());
                pref.setBudgetRange(row.getPreferredBudget());
                pref.setPurchaseTimeline(row.getPlanningToBuy());
                pref.setCreatedAt(lead.getCreatedTime() != null ? lead.getCreatedTime() : Instant.now());
                return leadPreferenceRepository.save(pref).thenReturn(lead);
            }));
    }

    private PlatformType parsePlatform(String raw) {
        if (raw == null) {
            return PlatformType.OTHER;
        }
        String p = raw.trim().toLowerCase();
        if (p.contains("ig") || p.contains("instagram")) {
            return PlatformType.IG;
        }
        if (p.contains("fb") || p.contains("facebook")) {
            return PlatformType.FB;
        }
        return PlatformType.OTHER;
    }

    private LeadStatus parseStatus(String raw) {
        if (raw == null) {
            return LeadStatus.CREATED;
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
