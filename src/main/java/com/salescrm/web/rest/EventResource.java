package com.salescrm.web.rest;

import com.salescrm.domain.CrmEvent;
import com.salescrm.service.CrmEventService;
import com.salescrm.service.FirebasePushNotificationService;
import com.salescrm.service.LeadAccessService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
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
    private final FirebasePushNotificationService firebasePushNotificationService;
    private final LeadAccessService leadAccessService;

    @Autowired
    public EventResource(
        CrmEventService crmEventService,
        @Autowired(required = false) LeadActivityService leadActivityService,
        @Autowired(required = false) FirebasePushNotificationService firebasePushNotificationService,
        LeadAccessService leadAccessService
    ) {
        this.crmEventService = crmEventService;
        this.leadActivityService = leadActivityService;
        this.firebasePushNotificationService = firebasePushNotificationService;
        this.leadAccessService = leadAccessService;
    }

    /**
     * A reminder is accessible to admins, to its owner, and (when it belongs to a candidate)
     * to anyone who has access to that candidate lead.
     */
    private Mono<Void> checkEventAccess(CrmEvent event) {
        return leadAccessService.currentScope().flatMap(scope -> {
            if (scope.isAdmin()) {
                return Mono.<Void>empty();
            }
            if (event.getUserId() != null && !scope.getLogin().isEmpty() && event.getUserId().equalsIgnoreCase(scope.getLogin())) {
                return Mono.<Void>empty();
            }
            if (event.getCandidateId() != null) {
                return leadAccessService.checkLeadAccess(event.getCandidateId());
            }
            return Mono.<Void>error(new AccessDeniedException("You do not have access to this reminder"));
        });
    }

    private Mono<CrmEvent> loadAuthorizedEvent(UUID id) {
        return crmEventService.getEventById(id).flatMap(event -> checkEventAccess(event).thenReturn(event));
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

        final Long candidateFilter = resolvedCandidateId;
        final String requestedUser = userId != null && !userId.isBlank() ? userId.trim() : null;

        return leadAccessService.currentScope().flatMap(scope -> {
            boolean ownOrAdmin = requestedUser == null || scope.isAdmin() || requestedUser.equalsIgnoreCase(scope.getLogin());
            if (!ownOrAdmin) {
                return Mono.<List<CrmEvent>>error(new AccessDeniedException("You can only view your own reminders"));
            }

            if (candidateFilter != null) {
                // Reminders for a candidate are visible to anyone with access to that candidate
                return leadAccessService
                    .checkLeadAccess(candidateFilter)
                    .then(Mono.defer(() -> crmEventService.getEvents(requestedUser, candidateFilter).collectList()));
            }

            String owner = requestedUser != null ? requestedUser : scope.getLogin();
            if (owner.isEmpty()) {
                return Mono.just(List.<CrmEvent>of());
            }
            return crmEventService.getEvents(owner, null).collectList();
        }).map(ResponseEntity::ok);
    }

    /**
     * {@code GET  /api/events/:id} : Get an event by ID.
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<CrmEvent>> getEvent(@PathVariable UUID id) {
        LOG.debug("REST request to get CrmEvent : {}", id);
        return ResponseUtil.wrapOrNotFound(loadAuthorizedEvent(id));
    }

    /**
     * {@code POST  /api/events} : Dispatch newly created reminder directly to PostgreSQL.
     */
    @PostMapping("")
    public Mono<ResponseEntity<CrmEvent>> createEvent(@Valid @RequestBody CrmEvent event) {
        LOG.debug("REST request to create CrmEvent : {}", event);
        boolean explicitUser = event.getUserId() != null && !event.getUserId().isBlank() && !"current".equalsIgnoreCase(event.getUserId());
        Mono<String> userResolver = leadAccessService.currentScope().flatMap(scope -> {
            String self = scope.getLogin().isEmpty() ? "admin" : scope.getLogin();
            Mono<Void> guard = event.getCandidateId() != null ? leadAccessService.checkLeadAccess(event.getCandidateId()) : Mono.empty();
            String owner;
            if (!explicitUser) {
                owner = self;
            } else if (scope.isAdmin() || event.getUserId().equalsIgnoreCase(self) || event.getCandidateId() != null) {
                // Staff may schedule a candidate reminder for that candidate's counselor
                owner = event.getUserId();
            } else {
                owner = self;
            }
            return guard.thenReturn(owner);
        });

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
        return loadAuthorizedEvent(id)
            .flatMap(existing ->
                event.getCandidateId() != null && !event.getCandidateId().equals(existing.getCandidateId())
                    ? leadAccessService.checkLeadAccess(event.getCandidateId()).thenReturn(existing)
                    : Mono.just(existing)
            )
            .flatMap(existing -> crmEventService.updateEvent(id, event))
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

        final Boolean desiredState = targetState;
        return loadAuthorizedEvent(id)
            .flatMap(existing -> crmEventService.toggleComplete(id, desiredState))
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * {@code DELETE  /api/events/:id} : Delete reminder from the backend.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteEvent(@PathVariable UUID id) {
        LOG.debug("REST request to delete CrmEvent : {}", id);
        return loadAuthorizedEvent(id)
            .flatMap(existing -> crmEventService.deleteEvent(id))
            .thenReturn(ResponseEntity.noContent().build());
    }
}
