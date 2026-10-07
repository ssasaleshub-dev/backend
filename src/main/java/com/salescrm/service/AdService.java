package com.salescrm.service;

import com.salescrm.service.dto.AdDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.salescrm.domain.Ad}.
 */
public interface AdService {
    /**
     * Save a ad.
     *
     * @param adDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<AdDTO> save(AdDTO adDTO);

    /**
     * Updates a ad.
     *
     * @param adDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<AdDTO> update(AdDTO adDTO);

    /**
     * Partially updates a ad.
     *
     * @param adDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<AdDTO> partialUpdate(AdDTO adDTO);

    /**
     * Get all the ads.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<AdDTO> findAll(Pageable pageable);

    /**
     * Get all the ads with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<AdDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of ads available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" ad.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<AdDTO> findOne(Long id);

    /**
     * Delete the "id" ad.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
