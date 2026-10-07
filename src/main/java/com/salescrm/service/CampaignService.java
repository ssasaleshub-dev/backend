package com.salescrm.service;

import com.salescrm.service.dto.CampaignDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.salescrm.domain.Campaign}.
 */
public interface CampaignService {
    /**
     * Save a campaign.
     *
     * @param campaignDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<CampaignDTO> save(CampaignDTO campaignDTO);

    /**
     * Updates a campaign.
     *
     * @param campaignDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<CampaignDTO> update(CampaignDTO campaignDTO);

    /**
     * Partially updates a campaign.
     *
     * @param campaignDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<CampaignDTO> partialUpdate(CampaignDTO campaignDTO);

    /**
     * Get all the campaigns.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<CampaignDTO> findAll(Pageable pageable);

    /**
     * Returns the number of campaigns available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" campaign.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<CampaignDTO> findOne(Long id);

    /**
     * Delete the "id" campaign.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
