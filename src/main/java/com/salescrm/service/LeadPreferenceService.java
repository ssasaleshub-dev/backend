package com.salescrm.service;

import com.salescrm.service.dto.LeadPreferenceDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.salescrm.domain.LeadPreference}.
 */
public interface LeadPreferenceService {
    /**
     * Save a leadPreference.
     *
     * @param leadPreferenceDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<LeadPreferenceDTO> save(LeadPreferenceDTO leadPreferenceDTO);

    /**
     * Updates a leadPreference.
     *
     * @param leadPreferenceDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<LeadPreferenceDTO> update(LeadPreferenceDTO leadPreferenceDTO);

    /**
     * Partially updates a leadPreference.
     *
     * @param leadPreferenceDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<LeadPreferenceDTO> partialUpdate(LeadPreferenceDTO leadPreferenceDTO);

    /**
     * Get all the leadPreferences.
     *
     * @return the list of entities.
     */
    Flux<LeadPreferenceDTO> findAll();

    /**
     * Get all the leadPreferences with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<LeadPreferenceDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of leadPreferences available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" leadPreference.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<LeadPreferenceDTO> findOne(Long id);

    /**
     * Delete the "id" leadPreference.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
