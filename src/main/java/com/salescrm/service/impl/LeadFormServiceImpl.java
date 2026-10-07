package com.salescrm.service.impl;

import com.salescrm.repository.LeadFormRepository;
import com.salescrm.service.LeadFormService;
import com.salescrm.service.dto.LeadFormDTO;
import com.salescrm.service.mapper.LeadFormMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.salescrm.domain.LeadForm}.
 */
@Service
@Transactional
public class LeadFormServiceImpl implements LeadFormService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadFormServiceImpl.class);

    private final LeadFormRepository leadFormRepository;

    private final LeadFormMapper leadFormMapper;

    public LeadFormServiceImpl(LeadFormRepository leadFormRepository, LeadFormMapper leadFormMapper) {
        this.leadFormRepository = leadFormRepository;
        this.leadFormMapper = leadFormMapper;
    }

    @Override
    public Mono<LeadFormDTO> save(LeadFormDTO leadFormDTO) {
        LOG.debug("Request to save LeadForm : {}", leadFormDTO);
        return leadFormRepository.save(leadFormMapper.toEntity(leadFormDTO)).map(leadFormMapper::toDto);
    }

    @Override
    public Mono<LeadFormDTO> update(LeadFormDTO leadFormDTO) {
        LOG.debug("Request to update LeadForm : {}", leadFormDTO);
        return leadFormRepository.save(leadFormMapper.toEntity(leadFormDTO)).map(leadFormMapper::toDto);
    }

    @Override
    public Mono<LeadFormDTO> partialUpdate(LeadFormDTO leadFormDTO) {
        LOG.debug("Request to partially update LeadForm : {}", leadFormDTO);

        return leadFormRepository
            .findById(leadFormDTO.getId())
            .map(existingLeadForm -> {
                leadFormMapper.partialUpdate(existingLeadForm, leadFormDTO);

                return existingLeadForm;
            })
            .flatMap(leadFormRepository::save)
            .map(leadFormMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<LeadFormDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all LeadForms");
        return leadFormRepository.findAllBy(pageable).map(leadFormMapper::toDto);
    }

    public Mono<Long> countAll() {
        return leadFormRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<LeadFormDTO> findOne(Long id) {
        LOG.debug("Request to get LeadForm : {}", id);
        return leadFormRepository.findById(id).map(leadFormMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete LeadForm : {}", id);
        return leadFormRepository.deleteById(id);
    }
}
