package com.salescrm.service.impl;

import com.salescrm.repository.AdSetRepository;
import com.salescrm.service.AdSetService;
import com.salescrm.service.dto.AdSetDTO;
import com.salescrm.service.mapper.AdSetMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.salescrm.domain.AdSet}.
 */
@Service
@Transactional
public class AdSetServiceImpl implements AdSetService {

    private static final Logger LOG = LoggerFactory.getLogger(AdSetServiceImpl.class);

    private final AdSetRepository adSetRepository;

    private final AdSetMapper adSetMapper;

    public AdSetServiceImpl(AdSetRepository adSetRepository, AdSetMapper adSetMapper) {
        this.adSetRepository = adSetRepository;
        this.adSetMapper = adSetMapper;
    }

    @Override
    public Mono<AdSetDTO> save(AdSetDTO adSetDTO) {
        LOG.debug("Request to save AdSet : {}", adSetDTO);
        return adSetRepository.save(adSetMapper.toEntity(adSetDTO)).map(adSetMapper::toDto);
    }

    @Override
    public Mono<AdSetDTO> update(AdSetDTO adSetDTO) {
        LOG.debug("Request to update AdSet : {}", adSetDTO);
        return adSetRepository.save(adSetMapper.toEntity(adSetDTO)).map(adSetMapper::toDto);
    }

    @Override
    public Mono<AdSetDTO> partialUpdate(AdSetDTO adSetDTO) {
        LOG.debug("Request to partially update AdSet : {}", adSetDTO);

        return adSetRepository
            .findById(adSetDTO.getId())
            .map(existingAdSet -> {
                adSetMapper.partialUpdate(existingAdSet, adSetDTO);

                return existingAdSet;
            })
            .flatMap(adSetRepository::save)
            .map(adSetMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdSetDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all AdSets");
        return adSetRepository.findAllBy(pageable).map(adSetMapper::toDto);
    }

    public Flux<AdSetDTO> findAllWithEagerRelationships(Pageable pageable) {
        return adSetRepository.findAllWithEagerRelationships(pageable).map(adSetMapper::toDto);
    }

    public Mono<Long> countAll() {
        return adSetRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<AdSetDTO> findOne(Long id) {
        LOG.debug("Request to get AdSet : {}", id);
        return adSetRepository.findOneWithEagerRelationships(id).map(adSetMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete AdSet : {}", id);
        return adSetRepository.deleteById(id);
    }
}
