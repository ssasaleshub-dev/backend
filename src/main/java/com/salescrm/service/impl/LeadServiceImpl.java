package com.salescrm.service.impl;

import com.salescrm.repository.LeadRepository;
import com.salescrm.service.LeadService;
import com.salescrm.service.dto.LeadDTO;
import com.salescrm.service.mapper.LeadMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
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

    public LeadServiceImpl(LeadRepository leadRepository, LeadMapper leadMapper) {
        this.leadRepository = leadRepository;
        this.leadMapper = leadMapper;
    }

    @Override
    public Mono<LeadDTO> save(LeadDTO leadDTO) {
        LOG.debug("Request to save Lead : {}", leadDTO);
        return leadRepository.save(leadMapper.toEntity(leadDTO)).map(leadMapper::toDto);
    }

    @Override
    public Mono<LeadDTO> update(LeadDTO leadDTO) {
        LOG.debug("Request to update Lead : {}", leadDTO);
        return leadRepository.save(leadMapper.toEntity(leadDTO)).map(leadMapper::toDto);
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
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Lead : {}", id);
        return leadRepository.deleteById(id);
    }
}
