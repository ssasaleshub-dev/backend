package com.salescrm.web.rest;

import com.salescrm.service.LeadDetailedService;
import com.salescrm.service.dto.LeadDetailedDTO;
import com.salescrm.service.dto.LeadStatusUpdateDTO;
import jakarta.validation.Valid;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for enriched lead views and lifecycle state transitions.
 */
@RestController
@RequestMapping("/api/leads")
public class LeadManagementResource {

    private static final Logger LOG = LoggerFactory.getLogger(LeadManagementResource.class);

    private final LeadDetailedService leadDetailedService;

    public LeadManagementResource(LeadDetailedService leadDetailedService) {
        this.leadDetailedService = leadDetailedService;
    }

    /**
     * {@code GET /api/leads/detailed} : Get all leads enriched with customer, attribution and preference data,
     * scoped to the authenticated user's role (or filtered by visaType for admin/unauthenticated).
     *
     * @param visaType optional visa filter (e.g. WORK_VISA, STUDY_VISA, DOMESTIC_COLLEGE_ADMISSION)
     * @return the list of detailed leads
     */
    @GetMapping("/detailed")
    public Mono<ResponseEntity<List<LeadDetailedDTO>>> getAllDetailedLeads(
        @RequestParam(name = "visaType", required = false) String visaType
    ) {
        LOG.debug("REST request to get enriched leads with visaType: {}", visaType);
        return leadDetailedService.findAllDetailed(visaType)
            .collectList()
            .map(ResponseEntity::ok);
    }

    /**
     * {@code GET /api/leads/detailed/:id} : Get single enriched lead by ID.
     *
     * @param id lead ID
     * @return the detailed lead
     */
    @GetMapping("/detailed/{id}")
    public Mono<ResponseEntity<LeadDetailedDTO>> getDetailedLead(@PathVariable Long id) {
        LOG.debug("REST request to get detailed lead: {}", id);
        return leadDetailedService.findDetailedById(id)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code PATCH /api/leads/:id/status} : Update the status and notes of a lead.
     *
     * @param id lead ID
     * @param updateDTO status update payload
     * @return the updated detailed lead
     */
    @PatchMapping("/{id}/status")
    public Mono<ResponseEntity<LeadDetailedDTO>> updateLeadStatus(
        @PathVariable Long id,
        @Valid @RequestBody LeadStatusUpdateDTO updateDTO
    ) {
        LOG.debug("REST request to update status for lead {}: {}", id, updateDTO.getStatus());
        return leadDetailedService.updateStatus(id, updateDTO.getStatus(), updateDTO.getNotes())
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code PATCH /api/leads/:id/assign} : Assign a lead to a staff member.
     *
     * @param id lead ID
     * @param payload map containing "assignedTo" staff login
     * @return the updated detailed lead
     */
    @PatchMapping("/{id}/assign")
    public Mono<ResponseEntity<LeadDetailedDTO>> assignLead(
        @PathVariable Long id,
        @RequestBody java.util.Map<String, String> payload
    ) {
        String assignedTo = payload != null ? payload.get("assignedTo") : null;
        LOG.debug("REST request to assign lead {} to staff {}", id, assignedTo);
        return leadDetailedService.updateAssignment(id, assignedTo)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
