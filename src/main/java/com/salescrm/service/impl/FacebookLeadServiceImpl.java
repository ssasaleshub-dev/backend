package com.salescrm.service.impl;

import com.salescrm.domain.*;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.domain.enumeration.PlatformType;
import com.salescrm.repository.*;
import com.salescrm.service.CrmNotificationService;
import com.salescrm.service.FacebookLeadService;
import com.salescrm.service.FirebasePushNotificationService;
import com.salescrm.service.LeadActivityService;
import com.salescrm.service.client.FacebookGraphApiClient;
import com.salescrm.service.dto.ExcelLeadRowDTO;
import com.salescrm.service.dto.LeadActivityDTO;
import com.salescrm.service.dto.facebook.FacebookWebhookPayloadDTO;
import com.salescrm.service.mapper.FacebookLeadMapper;
import com.salescrm.service.util.DateTimeParser;
import com.salescrm.service.util.PhoneNormalizer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Enterprise implementation of FacebookLeadService.
 * Orchestrates Meta Webhook payload processing, Graph API retrieval,
 * duplicate prevention (idempotency), customer deduplication, and PostgreSQL persistence.
 */
@Service
public class FacebookLeadServiceImpl implements FacebookLeadService {

    private static final Logger LOG = LoggerFactory.getLogger(FacebookLeadServiceImpl.class);

    private final FacebookGraphApiClient graphApiClient;
    private final FacebookLeadMapper leadMapper;
    private final LeadRepository leadRepository;
    private final CustomerRepository customerRepository;
    private final CampaignRepository campaignRepository;
    private final AdSetRepository adSetRepository;
    private final AdRepository adRepository;
    private final LeadFormRepository leadFormRepository;
    private final LeadPreferenceRepository leadPreferenceRepository;
    private final PhoneNormalizer phoneNormalizer;
    private final DateTimeParser dateTimeParser;
    private final LeadActivityService leadActivityService;
    private final FirebasePushNotificationService pushNotificationService;
    private final CrmNotificationService crmNotificationService;
    private final UserRepository userRepository;

    public FacebookLeadServiceImpl(
        FacebookGraphApiClient graphApiClient,
        FacebookLeadMapper leadMapper,
        LeadRepository leadRepository,
        CustomerRepository customerRepository,
        CampaignRepository campaignRepository,
        AdSetRepository adSetRepository,
        AdRepository adRepository,
        LeadFormRepository leadFormRepository,
        LeadPreferenceRepository leadPreferenceRepository,
        PhoneNormalizer phoneNormalizer,
        DateTimeParser dateTimeParser,
        LeadActivityService leadActivityService
    ) {
        this(
            graphApiClient,
            leadMapper,
            leadRepository,
            customerRepository,
            campaignRepository,
            adSetRepository,
            adRepository,
            leadFormRepository,
            leadPreferenceRepository,
            phoneNormalizer,
            dateTimeParser,
            leadActivityService,
            null,
            null,
            null
        );
    }

    @Autowired
    public FacebookLeadServiceImpl(
        FacebookGraphApiClient graphApiClient,
        FacebookLeadMapper leadMapper,
        LeadRepository leadRepository,
        CustomerRepository customerRepository,
        CampaignRepository campaignRepository,
        AdSetRepository adSetRepository,
        AdRepository adRepository,
        LeadFormRepository leadFormRepository,
        LeadPreferenceRepository leadPreferenceRepository,
        PhoneNormalizer phoneNormalizer,
        DateTimeParser dateTimeParser,
        @Autowired(required = false) LeadActivityService leadActivityService,
        @Autowired(required = false) FirebasePushNotificationService pushNotificationService,
        @Autowired(required = false) CrmNotificationService crmNotificationService,
        @Autowired(required = false) UserRepository userRepository
    ) {
        this.graphApiClient = graphApiClient;
        this.leadMapper = leadMapper;
        this.leadRepository = leadRepository;
        this.customerRepository = customerRepository;
        this.campaignRepository = campaignRepository;
        this.adSetRepository = adSetRepository;
        this.adRepository = adRepository;
        this.leadFormRepository = leadFormRepository;
        this.leadPreferenceRepository = leadPreferenceRepository;
        this.phoneNormalizer = phoneNormalizer;
        this.dateTimeParser = dateTimeParser;
        this.leadActivityService = leadActivityService;
        this.pushNotificationService = pushNotificationService;
        this.crmNotificationService = crmNotificationService;
        this.userRepository = userRepository;
    }

    @Override
    public Flux<Lead> processWebhookPayload(FacebookWebhookPayloadDTO payload) {
        if (payload == null || payload.getEntry() == null || payload.getEntry().isEmpty()) {
            LOG.warn("Received empty or invalid Meta webhook payload");
            return Flux.empty();
        }

        return Flux.fromIterable(payload.getEntry())
            .flatMap(entry -> {
                String pageId = entry.getId();
                if (entry.getChanges() == null || entry.getChanges().isEmpty()) {
                    return Flux.empty();
                }

                return Flux.fromIterable(entry.getChanges())
                    .filter(change -> "leadgen".equalsIgnoreCase(change.getField()) && change.getValue() != null)
                    .flatMap(change -> {
                        FacebookWebhookPayloadDTO.LeadgenValue val = change.getValue();
                        String leadgenId = val.getLeadgenId();
                        String adId = val.getAdId();
                        String formId = val.getFormId();
                        String effectivePageId = (val.getPageId() != null && !val.getPageId().isBlank())
                            ? val.getPageId()
                            : pageId;

                        LOG.info("Processing Meta leadgen event: leadgen_id={}, page_id={}, form_id={}, ad_id={}",
                            leadgenId, effectivePageId, formId, adId);

                        return processLead(leadgenId, effectivePageId, adId, formId);
                    });
            });
    }

    @Override
    @Transactional
    public Mono<Lead> processLead(String leadgenId, String pageId, String adId, String formId) {
        if (leadgenId == null || leadgenId.isBlank()) {
            LOG.warn("Skipping lead processing: leadgen_id is null or blank");
            return Mono.empty();
        }

        // Meta Developer Portal sends dummy leadgen_id="444444444444" when clicking the dashboard Test button
        if ("444444444444".equals(leadgenId) || leadgenId.matches("^4{6,}$")) {
            LOG.info("Received Meta Developer Dashboard mock test event (leadgen_id={}). Mock ID does not exist in Graph API. Acknowledged successfully.", leadgenId);
            return Mono.empty();
        }

        // Idempotency check: Guard against duplicate webhooks from Meta
        return leadRepository.findByExternalId(leadgenId)
            .flatMap(existing -> {
                LOG.info("Lead with external_id '{}' already exists in database (Lead ID: {}). Skipping duplicate insertion.",
                    leadgenId, existing.getId());
                return Mono.just(existing);
            })
            .switchIfEmpty(
                Mono.defer(() ->
                    graphApiClient.getLeadDetails(leadgenId)
                        .map(graphResponse -> {
                            if ((graphResponse.getAdId() == null || graphResponse.getAdId().isBlank()) && adId != null) {
                                graphResponse.setAdId(adId);
                            }
                            if ((graphResponse.getFormId() == null || graphResponse.getFormId().isBlank()) && formId != null) {
                                graphResponse.setFormId(formId);
                            }
                            return leadMapper.toRowDTO(graphResponse, pageId);
                        })
                        .flatMap(row -> persistLeadData(row, pageId))
                )
            )
            .doOnError(err -> LOG.error("Failed to process Meta lead {}: {}", leadgenId, err.getMessage(), err));
    }

    private Mono<Lead> persistLeadData(ExcelLeadRowDTO row, String pageId) {
        Instant createdTime = dateTimeParser.parse(row.getCreatedTime());
        if (createdTime == null) {
            createdTime = Instant.now();
        }

        String normalizedPhone = phoneNormalizer.normalize(row.getPhoneNumber());
        if (normalizedPhone == null || normalizedPhone.isBlank()) {
            normalizedPhone = "+910000000000"; // Fallback to satisfy NOT NULL if phone was absent in form
        }

        final Instant finalCreatedTime = createdTime;
        final String finalNormalizedPhone = normalizedPhone;

        return resolveCustomer(row, finalNormalizedPhone, finalCreatedTime)
            .flatMap(customer ->
                resolveCampaign(row, finalCreatedTime)
                    .flatMap(campaign -> resolveAdSet(row, finalCreatedTime, campaign))
                    .flatMap(adSet -> resolveAd(row, finalCreatedTime, adSet))
                    .flatMap(ad ->
                        resolveLeadForm(row, finalCreatedTime)
                            .flatMap(leadForm -> saveLead(row, finalCreatedTime, customer, ad, leadForm, pageId))
                    )
            )
            .flatMap(lead -> savePreference(row, lead))
            .doOnSuccess(savedLead -> {
                LOG.info("Successfully persisted Facebook lead: ID={}, external_id={}, customer_id={}",
                    savedLead.getId(), savedLead.getExternalId(), savedLead.getCustomerId());
                logLeadActivity(savedLead, row, pageId);
                dispatchNewLeadPushNotification(savedLead, row);
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
                if ((existing.getFullName() == null || existing.getFullName().isBlank() || "Facebook Lead".equalsIgnoreCase(existing.getFullName()))
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
                    : "Facebook Lead";
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
            return Mono.just(new Campaign());
        }

        return campaignRepository.findByExternalId(extId)
            .switchIfEmpty(Mono.defer(() -> {
                Campaign campaign = new Campaign();
                campaign.setExternalId(extId);
                String name = (row.getCampaignName() != null && !row.getCampaignName().isBlank())
                    ? row.getCampaignName().trim()
                    : "Meta Campaign " + extId;
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
            .flatMap(existing -> {
                // Replace an old placeholder ("Instant Form 123...") with the form's real name
                String current = existing.getName();
                if (current == null || current.isBlank() || current.startsWith("Instant Form ")) {
                    return graphApiClient.getFormName(extId)
                        .flatMap(realName -> {
                            existing.setName(realName);
                            return leadFormRepository.save(existing);
                        })
                        .defaultIfEmpty(existing);
                }
                return Mono.just(existing);
            })
            .switchIfEmpty(Mono.defer(() ->
                graphApiClient.getFormName(extId)
                    .defaultIfEmpty(
                        (row.getFormName() != null && !row.getFormName().isBlank() && !row.getFormName().startsWith("Instant Form "))
                            ? row.getFormName().trim()
                            : "Instant Form " + extId
                    )
                    .flatMap(name -> {
                        LeadForm form = new LeadForm();
                        form.setExternalId(extId);
                        form.setName(name);
                        form.setStatus("ACTIVE");
                        form.setCreatedAt(createdTime != null ? createdTime : Instant.now());
                        return leadFormRepository.save(form);
                    })
            ));
    }

    private Mono<Lead> saveLead(
        ExcelLeadRowDTO row,
        Instant createdTime,
        Customer customer,
        Ad ad,
        LeadForm leadForm,
        String pageId
    ) {
        PlatformType platform = parsePlatform(row.getPlatform());
        LeadStatus status = LeadStatus.CREATED;
        // Meta always sends is_organic explicitly for a real webhook/Graph API lead; a blank/missing
        // value here means the field truly was not reported, not that it is confirmed non-organic.
        // Leave it null rather than silently defaulting to false ("confirmed paid").
        Boolean isOrganic = parseTriStateBoolean(row.getIsOrganic());

        StringBuilder notesBuilder = new StringBuilder("Captured via Meta Lead Ads Webhook");
        if (pageId != null && !pageId.isBlank()) {
            notesBuilder.append(" | Page ID: ").append(pageId);
        }
        if (row.getCampaignName() != null && !row.getCampaignName().isBlank()) {
            notesBuilder.append(" | Campaign: ").append(row.getCampaignName().trim());
        }
        if (row.getCgpaOrPercentage() != null && !row.getCgpaOrPercentage().isBlank()) {
            notesBuilder.append(" | CGPA/Percentage: ").append(row.getCgpaOrPercentage().trim());
        }

        final String notes = notesBuilder.toString();

        Lead newLead = new Lead();
        newLead.setExternalId(row.getId());
        newLead.setCreatedTime(createdTime != null ? createdTime : Instant.now());
        newLead.setIsOrganic(isOrganic);
        newLead.setMetaPageId(pageId);
        newLead.setMetaFormResponses(row.getMetaFormResponses());
        newLead.setMetaRawPayload(row.getMetaRawPayload());
        newLead.setPlatform(platform);
        newLead.setStatus(status);
        newLead.setNotes(notes);
        newLead.setCreatedAt(Instant.now());
        newLead.setUpdatedAt(Instant.now());

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
    }

    private Mono<Lead> savePreference(ExcelLeadRowDTO row, Lead lead) {
        if (lead == null || lead.getId() == null) {
            return Mono.justOrEmpty(lead);
        }

        return leadPreferenceRepository.findOneByLeadId(lead.getId())
            .switchIfEmpty(Mono.defer(() -> {
                LeadPreference pref = new LeadPreference();
                pref.setLead(lead);
                pref.setVisaType(row.getVisaType());
                pref.setAge(row.getAge());
                pref.setEducationalQualification(row.getEducationalQualification());
                pref.setAreYouInterested(row.getAreYouInterested());
                pref.setAreYouWorking(row.getAreYouWorking());
                pref.setLeadConfirm(row.getLeadConfirm());

                if (row.getCgpaOrPercentage() != null && !row.getCgpaOrPercentage().isBlank()) {
                    pref.setBudgetRange(row.getCgpaOrPercentage().trim());
                }

                if (row.getPreferredBudget() != null && !row.getPreferredBudget().isBlank()) {
                    pref.setBudgetRange(row.getPreferredBudget());
                }
                pref.setPurchaseTimeline(row.getPlanningToBuy());
                pref.setCreatedAt(Instant.now());

                return leadPreferenceRepository.save(pref);
            }))
            .thenReturn(lead);
    }

    private void logLeadActivity(Lead lead, ExcelLeadRowDTO row, String pageId) {
        if (leadActivityService == null || lead == null || lead.getId() == null) {
            return;
        }

        LeadActivityDTO act = new LeadActivityDTO();
        act.setLeadId(lead.getId());
        act.setActionType("LEAD_CAPTURED");
        act.setSummary("Facebook Lead Ad received via Webhook");
        act.setDetails(String.format("Captured from Page: %s, Campaign: %s, Form: %s, Customer: %s",
            pageId != null ? pageId : "N/A",
            row.getCampaignName() != null ? row.getCampaignName() : "N/A",
            row.getFormId() != null ? row.getFormId() : "N/A",
            row.getFullName() != null ? row.getFullName() : "N/A"
        ));
        act.setUserId("system");
        act.setUserName("Meta Webhook");
        act.setCreatedAt(Instant.now());

        leadActivityService.logActivity(act)
            .subscribe(
                activity -> LOG.debug("Logged lead activity {} for lead ID {}", activity.getId(), lead.getId()),
                err -> LOG.warn("Failed to log lead activity for lead ID {}: {}", lead.getId(), err.getMessage())
            );
    }

    /**
     * Parses a true/false signal that genuinely distinguishes "known true", "known false", and
     * "not reported" — unlike {@code Boolean.parseBoolean}, which collapses anything that isn't
     * exactly "true" into false, silently turning "unknown" into "confirmed false".
     */
    private Boolean parseTriStateBoolean(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String r = raw.trim().toLowerCase();
        if (r.equals("true") || r.equals("1") || r.equals("yes")) {
            return Boolean.TRUE;
        }
        if (r.equals("false") || r.equals("0") || r.equals("no")) {
            return Boolean.FALSE;
        }
        return null;
    }

    private PlatformType parsePlatform(String raw) {
        if (raw == null || raw.isBlank()) {
            return PlatformType.FB;
        }
        String p = raw.trim().toLowerCase();
        if (p.contains("ig") || p.contains("instagram")) {
            return PlatformType.IG;
        }
        if (p.contains("whatsapp") || p.contains("wa")) {
            return PlatformType.WHATSAPP;
        }
        return PlatformType.FB;
    }

    private void dispatchNewLeadPushNotification(Lead savedLead, ExcelLeadRowDTO row) {
        if (pushNotificationService == null || savedLead == null) {
            return;
        }
        try {
            String name = (row != null && row.getFullName() != null && !row.getFullName().isBlank())
                ? row.getFullName()
                : "New Candidate";
            String phone = (row != null && row.getPhoneNumber() != null && !row.getPhoneNumber().isBlank())
                ? row.getPhoneNumber()
                : "";
            String formName = (row != null && row.getFormName() != null && !row.getFormName().isBlank())
                ? row.getFormName()
                : "Meta Lead Ad";

            String title = "New Lead Received: " + name;
            String body = formName + (!phone.isBlank() ? " • " + phone : "");

            Map<String, String> data = new HashMap<>();
            data.put("type", "NEW_LEAD");
            data.put("leadId", savedLead.getId() != null ? savedLead.getId().toString() : "");
            data.put("externalId", savedLead.getExternalId() != null ? savedLead.getExternalId() : "");
            data.put("candidateName", name);
            data.put("phoneNumber", phone);
            data.put("route", "/leads");

            if (pushNotificationService != null) {
                pushNotificationService.sendPushNotificationToAll(title, body, data);
            }

            // MNC Standard: Persist in-app notification in crm_notifications table for all staff
            if (crmNotificationService != null && userRepository != null) {
                userRepository.findAll()
                    .flatMap(user -> {
                        CrmNotification notif = new CrmNotification();
                        notif.setUserId(user.getLogin().toLowerCase());
                        notif.setTitle(title);
                        notif.setMessage(body);
                        notif.setType("NEW_LEAD");
                        notif.setReferenceId(savedLead.getId() != null ? savedLead.getId().toString() : null);
                        notif.setReferenceType("LEAD");
                        notif.setIsRead(Boolean.FALSE);
                        notif.setCreatedAt(Instant.now());
                        return crmNotificationService.createNotification(notif);
                    })
                    .subscribe(
                        saved -> LOG.debug("[FacebookLeadNotification] Persisted in-app lead notification for user: {}", saved.getUserId()),
                        err -> LOG.error("[FacebookLeadNotification] Failed to persist in-app lead notification: {}", err.getMessage())
                    );
            }
        } catch (Exception e) {
            LOG.error("Failed to dispatch push notification for new lead {}: {}", savedLead.getId(), e.getMessage());
        }
    }
}
