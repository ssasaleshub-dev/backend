package com.salescrm.service;

import com.salescrm.service.dto.LeadDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.salescrm.domain.Lead}.
 */
public interface LeadService {
    /**
     * Save a lead.
     *
     * @param leadDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<LeadDTO> save(LeadDTO leadDTO);

    /**
     * Updates a lead.
     *
     * @param leadDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<LeadDTO> update(LeadDTO leadDTO);

    /**
     * Partially updates a lead.
     *
     * @param leadDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<LeadDTO> partialUpdate(LeadDTO leadDTO);

    /**
     * Get all the leads.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<LeadDTO> findAll(Pageable pageable);

    /**
     * Get all the LeadDTO where LeadPreference is {@code null}.
     *
     * @return the {@link Flux} of entities.
     */
    Flux<LeadDTO> findAllWhereLeadPreferenceIsNull();

    /**
     * Get all the leads with eager load of many-to-many relationships.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<LeadDTO> findAllWithEagerRelationships(Pageable pageable);

    /**
     * Returns the number of leads available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" lead.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<LeadDTO> findOne(Long id);

    /**
     * Delete the "id" lead.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
