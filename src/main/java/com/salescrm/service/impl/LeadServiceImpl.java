package com.salescrm.service.impl;

import com.salescrm.domain.Lead;
import com.salescrm.repository.LeadRepository;
import com.salescrm.service.FirebasePushNotificationService;
import com.salescrm.service.LeadService;
import com.salescrm.service.dto.LeadDTO;
import com.salescrm.service.mapper.LeadMapper;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.salescrm.domain.Lead}.
 */
@Service
@Transactional
public class LeadServiceImpl implements LeadService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadServiceImpl.class);

    private final LeadRepository leadRepository;

    private final LeadMapper leadMapper;

    private final DatabaseClient databaseClient;

    private final FirebasePushNotificationService pushNotificationService;

    public LeadServiceImpl(LeadRepository leadRepository, LeadMapper leadMapper, DatabaseClient databaseClient) {
        this(leadRepository, leadMapper, databaseClient, null);
    }

    @Autowired
    public LeadServiceImpl(
        LeadRepository leadRepository,
        LeadMapper leadMapper,
        DatabaseClient databaseClient,
        @Autowired(required = false) FirebasePushNotificationService pushNotificationService
    ) {
        this.leadRepository = leadRepository;
        this.leadMapper = leadMapper;
        this.databaseClient = databaseClient;
        this.pushNotificationService = pushNotificationService;
    }

    @Override
    public Mono<LeadDTO> save(LeadDTO leadDTO) {
        LOG.debug("Request to save Lead : {}", leadDTO);
        if (leadDTO.getExternalId() != null && !leadDTO.getExternalId().isBlank()) {
            String cleanExtId = leadDTO.getExternalId().trim();
            return leadRepository
                .findByExternalId(cleanExtId)
                .flatMap(existingLead -> {
                    leadMapper.partialUpdate(existingLead, leadDTO);
                    if (leadDTO.getCustomer() != null && leadDTO.getCustomer().getId() != null) {
                        existingLead.setCustomerId(leadDTO.getCustomer().getId());
                    }
                    if (leadDTO.getAd() != null && leadDTO.getAd().getId() != null) {
                        existingLead.setAdId(leadDTO.getAd().getId());
                    }
                    if (leadDTO.getLeadForm() != null && leadDTO.getLeadForm().getId() != null) {
                        existingLead.setLeadFormId(leadDTO.getLeadForm().getId());
                    }
                    return leadRepository.save(existingLead);
                })
                .switchIfEmpty(Mono.defer(() -> leadRepository.save(leadMapper.toEntity(leadDTO))))
                .map(leadMapper::toDto);
        }
        return leadRepository.save(leadMapper.toEntity(leadDTO))
            .map(leadMapper::toDto)
            .doOnSuccess(this::dispatchNewLeadPushNotification);
    }

    private void dispatchNewLeadPushNotification(LeadDTO savedLead) {
        if (pushNotificationService == null || savedLead == null) {
            return;
        }
        try {
            String name = (savedLead.getCustomer() != null && savedLead.getCustomer().getFullName() != null)
                ? savedLead.getCustomer().getFullName()
                : "New Lead";
            String title = "🎯 New Lead Received: " + name;
            String body = "New lead added to CRM";

            Map<String, String> data = new HashMap<>();
            data.put("type", "NEW_LEAD");
            data.put("leadId", savedLead.getId() != null ? savedLead.getId().toString() : "");
            data.put("candidateName", name);
            data.put("route", "/leads");

            pushNotificationService.sendPushNotificationToAll(title, body, data);
        } catch (Exception e) {
            LOG.error("Failed to dispatch push notification for lead: {}", e.getMessage());
        }
    }

    @Override
    public Mono<LeadDTO> update(LeadDTO leadDTO) {
        LOG.debug("Request to update Lead : {}", leadDTO);
        return leadRepository.save(leadMapper.toEntity(leadDTO))
            .map(leadMapper::toDto)
            .doOnSuccess(this::dispatchNewLeadPushNotification);
    }

    @Override
    public Mono<LeadDTO> partialUpdate(LeadDTO leadDTO) {
        LOG.debug("Request to partially update Lead : {}", leadDTO);

        return leadRepository
            .findById(leadDTO.getId())
            .map(existingLead -> {
                leadMapper.partialUpdate(existingLead, leadDTO);

                return existingLead;
            })
            .flatMap(leadRepository::save)
            .map(leadMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<LeadDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Leads");
        return leadRepository.findAllBy(pageable).map(leadMapper::toDto);
    }

    public Flux<LeadDTO> findAllWithEagerRelationships(Pageable pageable) {
        return leadRepository.findAllWithEagerRelationships(pageable).map(leadMapper::toDto);
    }

    /**
     *  Get all the leads where LeadPreference is {@code null}.
     *  @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Flux<LeadDTO> findAllWhereLeadPreferenceIsNull() {
        LOG.debug("Request to get all leads where LeadPreference is null");
        return leadRepository.findAllWhereLeadPreferenceIsNull().map(leadMapper::toDto);
    }

    public Mono<Long> countAll() {
        return leadRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<LeadDTO> findOne(Long id) {
        LOG.debug("Request to get Lead : {}", id);
        return leadRepository.findOneWithEagerRelationships(id).map(leadMapper::toDto);
    }

    @Override
    @Transactional
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Lead : {}", id);
        return databaseClient.sql("DELETE FROM lead_activity WHERE lead_id = :id")
            .bind("id", id)
            .fetch()
            .rowsUpdated()
            .then(
                databaseClient.sql("DELETE FROM lead_preference WHERE lead_id = :id")
                    .bind("id", id)
                    .fetch()
                    .rowsUpdated()
            )
            .then(
                databaseClient.sql("DELETE FROM crm_events_reminders WHERE candidate_id = :id")
                    .bind("id", id)
                    .fetch()
                    .rowsUpdated()
            )
            .then(leadRepository.deleteById(id));
    }
}
