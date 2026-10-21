package com.salescrm.service;

import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.service.dto.LeadDetailedDTO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service interface for querying and managing fully-resolved leads
 * with customer profile, marketing attribution, and questionnaire responses.
 */
public interface LeadDetailedService {

    /**
     * Finds all detailed leads ordered by creation time descending.
     *
     * @return Flux of LeadDetailedDTO
     */
    Flux<LeadDetailedDTO> findAllDetailed();

    /**
     * Finds all detailed leads matching optional visa type filter, scoped to the current user's role.
     *
     * @param visaTypeFilter optional visa type filter (WORK_VISA, STUDY_VISA, etc.)
     * @return Flux of LeadDetailedDTO
     */
    Flux<LeadDetailedDTO> findAllDetailed(String visaTypeFilter);

    /**
     * Finds a single detailed lead by primary ID.
     *
     * @param id lead ID
     * @return Mono of LeadDetailedDTO
     */
    Mono<LeadDetailedDTO> findDetailedById(Long id);

    /**
     * Updates the status and optional notes of a lead.
     *
     * @param id lead ID
     * @param status new status
     * @param notes updated notes
     * @return Mono of updated LeadDetailedDTO
     */
    Mono<LeadDetailedDTO> updateStatus(Long id, LeadStatus status, String notes);

    /**
     * Assigns a lead to a staff member.
     *
     * @param id lead ID
     * @param assignedTo staff username
     * @return Mono of updated LeadDetailedDTO
     */
    Mono<LeadDetailedDTO> updateAssignment(Long id, String assignedTo);
}
