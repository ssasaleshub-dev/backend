package com.salescrm.web.rest;

import com.salescrm.domain.CrmEvent;
import com.salescrm.security.SecurityUtils;
import com.salescrm.service.CrmEventService;
import com.salescrm.service.LeadActivityService;
import com.salescrm.service.dto.LeadActivityDTO;
import com.salescrm.service.util.LeadIdCrypto;
import jakarta.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link CrmEvent} (Calendar events, reminders, callback schedules).
 */
@RestController
@RequestMapping("/api/events")
public class EventResource {

    private static final Logger LOG = LoggerFactory.getLogger(EventResource.class);

    private final CrmEventService crmEventService;
    private final LeadActivityService leadActivityService;

    public EventResource(CrmEventService crmEventService) {
        this(crmEventService, null);
    }

    public EventResource(CrmEventService crmEventService, LeadActivityService leadActivityService) {
        this.crmEventService = crmEventService;
        this.leadActivityService = leadActivityService;
    }

    /**
     * {@code GET  /api/events} : Get dynamic calendar events & counselor callback schedules.
     */
    @GetMapping("")
    public Mono<ResponseEntity<List<CrmEvent>>> getEvents(
        @RequestParam(name = "userId", required = false) String userId,
        @RequestParam(name = "candidateId", required = false) String candidateId
    ) {
        LOG.debug("REST request to get events. userId: {}, candidateId: {}", userId, candidateId);

        Long resolvedCandidateId = null;
        if (candidateId != null && !candidateId.isBlank()) {
            resolvedCandidateId = LeadIdCrypto.decrypt(candidateId);
            if (resolvedCandidateId == null) {
                try { resolvedCandidateId = Long.parseLong(candidateId.trim()); } catch (NumberFormatException ignored) {}
            }
        }

        if (userId != null && !userId.isBlank()) {
            return crmEventService.getEvents(userId, resolvedCandidateId).collectList().map(ResponseEntity::ok);
        }

        if (resolvedCandidateId != null) {
            return crmEventService.getEvents(null, resolvedCandidateId).collectList().map(ResponseEntity::ok);
        }

        return SecurityUtils.getCurrentUserLogin()
            .flatMap(login -> crmEventService.getEvents(login, null).collectList().map(ResponseEntity::ok))
            .switchIfEmpty(Mono.defer(() -> crmEventService.getEvents(null, null).collectList().map(ResponseEntity::ok)));
    }

    /**
     * {@code GET  /api/events/:id} : Get an event by ID.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<CrmEvent>> getEvent(@PathVariable UUID id) {
        LOG.debug("REST request to get CrmEvent : {}", id);
        return ResponseUtil.wrapOrNotFound(crmEventService.getEventById(id));
    }

    /**
     * {@code POST  /api/events} : Dispatch newly created reminder directly to PostgreSQL.
     */
    @PostMapping("")
    public Mono<ResponseEntity<CrmEvent>> createEvent(@Valid @RequestBody CrmEvent event) {
        LOG.debug("REST request to create CrmEvent : {}", event);
        Mono<String> userResolver = (event.getUserId() != null && !event.getUserId().isBlank())
            ? Mono.just(event.getUserId())
            : SecurityUtils.getCurrentUserLogin().defaultIfEmpty("admin");

        return userResolver.flatMap(userId -> {
            event.setUserId(userId);
            return crmEventService
                .createEvent(event)
                .map(result -> {
                    try {
                        if (leadActivityService != null && result.getCandidateId() != null) {
                            LeadActivityDTO act = new LeadActivityDTO();
                            act.setLeadId(result.getCandidateId());
                            act.setUserName(result.getUserId() != null ? result.getUserId() : "Staff");
                            act.setActionType("REMARKS_ADDED");
                            act.setSummary("Reminder Scheduled: " + result.getTitle());
                            act.setDetails("Scheduled on " + result.getEventDate() + " at " + (result.getEventTime() != null ? result.getEventTime() : "09:00") + (result.getDescription() != null ? " - " + result.getDescription() : ""));
                            leadActivityService.logActivity(act).subscribe(null, err -> LOG.warn("Could not log reminder activity: {}", err.getMessage()));
                        }
                        return ResponseEntity.created(new URI("/api/events/" + result.getId())).body(result);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                });
        });
    }

    /**
     * {@code PUT  /api/events/:id} : Update event/time.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<CrmEvent>> updateEvent(@PathVariable UUID id, @RequestBody CrmEvent event) {
        LOG.debug("REST request to update CrmEvent : {}, data: {}", id, event);
        return crmEventService
            .updateEvent(id, event)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code PATCH  /api/events/:id/toggle-complete} : Mark reminder completed dynamically.
     */
    @PatchMapping("/{id}/toggle-complete")
    public Mono<ResponseEntity<CrmEvent>> toggleComplete(
        @PathVariable UUID id,
        @RequestParam(name = "completed", required = false) Boolean completed,
        @RequestBody(required = false) Map<String, Object> body
    ) {
        LOG.debug("REST request to toggle complete for CrmEvent : {}, completed param: {}, body: {}", id, completed, body);
        Boolean targetState = completed;
        if (targetState == null && body != null) {
            if (body.containsKey("isCompleted")) {
                targetState = Boolean.valueOf(String.valueOf(body.get("isCompleted")));
            } else if (body.containsKey("completed")) {
                targetState = Boolean.valueOf(String.valueOf(body.get("completed")));
            }
        }

        return crmEventService
            .toggleComplete(id, targetState)
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code DELETE  /api/events/:id} : Delete reminder from the backend.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteEvent(@PathVariable UUID id) {
        LOG.debug("REST request to delete CrmEvent : {}", id);
        return crmEventService.deleteEvent(id).thenReturn(ResponseEntity.noContent().build());
    }
}
