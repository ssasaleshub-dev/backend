package com.salescrm.web.rest;

import com.salescrm.domain.LeadActivity;
import com.salescrm.service.LeadActivityService;
import com.salescrm.service.dto.LeadActivityDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for managing Lead Activity and Audit Logs.
 */
@RestController
@RequestMapping("/api/leads")
public class LeadActivityResource {

    private static final Logger LOG = LoggerFactory.getLogger(LeadActivityResource.class);

    private final LeadActivityService leadActivityService;

    public LeadActivityResource(LeadActivityService leadActivityService) {
        this.leadActivityService = leadActivityService;
    }

    /**
     * {@code GET /api/leads/{id}/activities} : Get all activity logs for a candidate/lead.
     *
     * @param id the id of the lead
     * @return the {@link Flux} of activities
     */
    @GetMapping(value = "/{id}/activities", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<LeadActivity> getLeadActivities(@PathVariable("id") Long id) {
        LOG.debug("REST request to get Lead Activities for lead ID: {}", id);
        return leadActivityService.getActivitiesForLead(id);
    }

    /**
     * {@code POST /api/leads/{id}/activities} : Record a new activity or audit event for a candidate/lead.
     *
     * @param id the id of the lead
     * @param leadActivityDTO the activity details
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new activity
     */
    @PostMapping(value = "/{id}/activities", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<LeadActivity>> createLeadActivity(
        @PathVariable("id") Long id,
        @Valid @RequestBody LeadActivityDTO leadActivityDTO
    ) {
        LOG.debug("REST request to record Lead Activity for lead ID: {}", id);
        leadActivityDTO.setLeadId(id);

        return leadActivityService.logActivity(leadActivityDTO)
            .map(result -> ResponseEntity.status(HttpStatus.CREATED).body(result));
    }
}
