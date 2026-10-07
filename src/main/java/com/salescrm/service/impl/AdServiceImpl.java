package com.salescrm.service.impl;

import com.salescrm.repository.AdRepository;
import com.salescrm.service.AdService;
import com.salescrm.service.dto.AdDTO;
import com.salescrm.service.mapper.AdMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.salescrm.domain.Ad}.
 */
@Service
@Transactional
public class AdServiceImpl implements AdService {

    private static final Logger LOG = LoggerFactory.getLogger(AdServiceImpl.class);

    private final AdRepository adRepository;

    private final AdMapper adMapper;

    public AdServiceImpl(AdRepository adRepository, AdMapper adMapper) {
        this.adRepository = adRepository;
        this.adMapper = adMapper;
    }

    @Override
    public Mono<AdDTO> save(AdDTO adDTO) {
        LOG.debug("Request to save Ad : {}", adDTO);
        return adRepository.save(adMapper.toEntity(adDTO)).map(adMapper::toDto);
    }

    @Override
    public Mono<AdDTO> update(AdDTO adDTO) {
        LOG.debug("Request to update Ad : {}", adDTO);
        return adRepository.save(adMapper.toEntity(adDTO)).map(adMapper::toDto);
    }

    @Override
    public Mono<AdDTO> partialUpdate(AdDTO adDTO) {
        LOG.debug("Request to partially update Ad : {}", adDTO);

        return adRepository
            .findById(adDTO.getId())
            .map(existingAd -> {
                adMapper.partialUpdate(existingAd, adDTO);

                return existingAd;
            })
            .flatMap(adRepository::save)
            .map(adMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<AdDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Ads");
        return adRepository.findAllBy(pageable).map(adMapper::toDto);
    }

    public Flux<AdDTO> findAllWithEagerRelationships(Pageable pageable) {
        return adRepository.findAllWithEagerRelationships(pageable).map(adMapper::toDto);
    }

    public Mono<Long> countAll() {
        return adRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<AdDTO> findOne(Long id) {
        LOG.debug("Request to get Ad : {}", id);
        return adRepository.findOneWithEagerRelationships(id).map(adMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Ad : {}", id);
        return adRepository.deleteById(id);
    }
}
