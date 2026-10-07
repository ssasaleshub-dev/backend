package com.salescrm.service;

import com.salescrm.service.dto.LeadFormDTO;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Interface for managing {@link com.salescrm.domain.LeadForm}.
 */
public interface LeadFormService {
    /**
     * Save a leadForm.
     *
     * @param leadFormDTO the entity to save.
     * @return the persisted entity.
     */
    Mono<LeadFormDTO> save(LeadFormDTO leadFormDTO);

    /**
     * Updates a leadForm.
     *
     * @param leadFormDTO the entity to update.
     * @return the persisted entity.
     */
    Mono<LeadFormDTO> update(LeadFormDTO leadFormDTO);

    /**
     * Partially updates a leadForm.
     *
     * @param leadFormDTO the entity to update partially.
     * @return the persisted entity.
     */
    Mono<LeadFormDTO> partialUpdate(LeadFormDTO leadFormDTO);

    /**
     * Get all the leadForms.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    Flux<LeadFormDTO> findAll(Pageable pageable);

    /**
     * Returns the number of leadForms available.
     * @return the number of entities in the database.
     *
     */
    Mono<Long> countAll();

    /**
     * Get the "id" leadForm.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    Mono<LeadFormDTO> findOne(Long id);

    /**
     * Delete the "id" leadForm.
     *
     * @param id the id of the entity.
     * @return a Mono to signal the deletion
     */
    Mono<Void> delete(Long id);
}
