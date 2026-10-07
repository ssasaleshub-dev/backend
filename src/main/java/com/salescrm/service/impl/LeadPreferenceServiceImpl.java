package com.salescrm.service.impl;

import com.salescrm.repository.LeadPreferenceRepository;
import com.salescrm.service.LeadPreferenceService;
import com.salescrm.service.dto.LeadPreferenceDTO;
import com.salescrm.service.mapper.LeadPreferenceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.salescrm.domain.LeadPreference}.
 */
@Service
@Transactional
public class LeadPreferenceServiceImpl implements LeadPreferenceService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadPreferenceServiceImpl.class);

    private final LeadPreferenceRepository leadPreferenceRepository;

    private final LeadPreferenceMapper leadPreferenceMapper;

    public LeadPreferenceServiceImpl(LeadPreferenceRepository leadPreferenceRepository, LeadPreferenceMapper leadPreferenceMapper) {
        this.leadPreferenceRepository = leadPreferenceRepository;
        this.leadPreferenceMapper = leadPreferenceMapper;
    }

    @Override
    public Mono<LeadPreferenceDTO> save(LeadPreferenceDTO leadPreferenceDTO) {
        LOG.debug("Request to save LeadPreference : {}", leadPreferenceDTO);
        return leadPreferenceRepository.save(leadPreferenceMapper.toEntity(leadPreferenceDTO)).map(leadPreferenceMapper::toDto);
    }

    @Override
    public Mono<LeadPreferenceDTO> update(LeadPreferenceDTO leadPreferenceDTO) {
        LOG.debug("Request to update LeadPreference : {}", leadPreferenceDTO);
        return leadPreferenceRepository.save(leadPreferenceMapper.toEntity(leadPreferenceDTO)).map(leadPreferenceMapper::toDto);
    }

    @Override
    public Mono<LeadPreferenceDTO> partialUpdate(LeadPreferenceDTO leadPreferenceDTO) {
        LOG.debug("Request to partially update LeadPreference : {}", leadPreferenceDTO);

        return leadPreferenceRepository
            .findById(leadPreferenceDTO.getId())
            .map(existingLeadPreference -> {
                leadPreferenceMapper.partialUpdate(existingLeadPreference, leadPreferenceDTO);

                return existingLeadPreference;
            })
            .flatMap(leadPreferenceRepository::save)
            .map(leadPreferenceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<LeadPreferenceDTO> findAll() {
        LOG.debug("Request to get all LeadPreferences");
        return leadPreferenceRepository.findAll().map(leadPreferenceMapper::toDto);
    }

    public Flux<LeadPreferenceDTO> findAllWithEagerRelationships(Pageable pageable) {
        return leadPreferenceRepository.findAllWithEagerRelationships(pageable).map(leadPreferenceMapper::toDto);
    }

    public Mono<Long> countAll() {
        return leadPreferenceRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<LeadPreferenceDTO> findOne(Long id) {
        LOG.debug("Request to get LeadPreference : {}", id);
        return leadPreferenceRepository.findOneWithEagerRelationships(id).map(leadPreferenceMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete LeadPreference : {}", id);
        return leadPreferenceRepository.deleteById(id);
    }
}
