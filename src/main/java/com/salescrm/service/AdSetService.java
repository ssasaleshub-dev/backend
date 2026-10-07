package com.salescrm.service;

import com.salescrm.service.dto.AdSetDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.salescrm.domain.AdSet}.
 */
public interface AdSetService {
    /**
     * Save a adSet.
     *
     * @param adSetDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<AdSetDTO> save(AdSetDTO adSetDTO);

    /**
     * Updates a adSet.
     *
     * @param adSetDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<AdSetDTO> update(AdSetDTO adSetDTO);

    /**
     * Partially updates a adSet.
     *
     * @param adSetDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<AdSetDTO> partialUpdate(AdSetDTO adSetDTO);

    /**
     * Get all the adSets.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<AdSetDTO> findAll(Pageable pageable);

    /**
     * Get all the adSets with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<AdSetDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of adSets available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" adSet.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<AdSetDTO> findOne(Long id);

    /**
     * Delete the "id" adSet.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
