package com.salescrm.service.impl;

import com.salescrm.repository.CampaignRepository;
import com.salescrm.service.CampaignService;
import com.salescrm.service.dto.CampaignDTO;
import com.salescrm.service.mapper.CampaignMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing {@link com.salescrm.domain.Campaign}.
 */
@Service
@Transactional
public class CampaignServiceImpl implements CampaignService {

    private static final Logger LOG = LoggerFactory.getLogger(CampaignServiceImpl.class);

    private final CampaignRepository campaignRepository;

    private final CampaignMapper campaignMapper;

    public CampaignServiceImpl(CampaignRepository campaignRepository, CampaignMapper campaignMapper) {
        this.campaignRepository = campaignRepository;
        this.campaignMapper = campaignMapper;
    }

    @Override
    public Mono<CampaignDTO> save(CampaignDTO campaignDTO) {
        LOG.debug("Request to save Campaign : {}", campaignDTO);
        return campaignRepository.save(campaignMapper.toEntity(campaignDTO)).map(campaignMapper::toDto);
    }

    @Override
    public Mono<CampaignDTO> update(CampaignDTO campaignDTO) {
        LOG.debug("Request to update Campaign : {}", campaignDTO);
        return campaignRepository.save(campaignMapper.toEntity(campaignDTO)).map(campaignMapper::toDto);
    }

    @Override
    public Mono<CampaignDTO> partialUpdate(CampaignDTO campaignDTO) {
        LOG.debug("Request to partially update Campaign : {}", campaignDTO);

        return campaignRepository
            .findById(campaignDTO.getId())
            .map(existingCampaign -> {
                campaignMapper.partialUpdate(existingCampaign, campaignDTO);

                return existingCampaign;
            })
            .flatMap(campaignRepository::save)
            .map(campaignMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<CampaignDTO> findAll(Pageable pageable) {
        LOG.debug("Request to get all Campaigns");
        return campaignRepository.findAllBy(pageable).map(campaignMapper::toDto);
    }

    public Mono<Long> countAll() {
        return campaignRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<CampaignDTO> findOne(Long id) {
        LOG.debug("Request to get Campaign : {}", id);
        return campaignRepository.findById(id).map(campaignMapper::toDto);
    }

    @Override
    public Mono<Void> delete(Long id) {
        LOG.debug("Request to delete Campaign : {}", id);
        return campaignRepository.deleteById(id);
    }
}
