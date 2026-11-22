package com.salescrm.service;

import com.salescrm.domain.LeadPreference;
import com.salescrm.repository.LeadPreferenceRepository;
import com.salescrm.security.LeadAccessScope;
import com.salescrm.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Central server-side authorization for lead data. A lead's visa type and assignee live on its
 * {@link LeadPreference}; every endpoint that reads or changes a lead (or its preference,
 * activities or reminders) must go through here. Denials surface as HTTP 403.
 */
@Service
public class LeadAccessService {

    private final LeadPreferenceRepository preferenceRepository;

    public LeadAccessService(LeadPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    public Mono<LeadAccessScope> currentScope() {
        return Mono.zip(
            SecurityUtils.getCurrentUserLogin().defaultIfEmpty(""),
            SecurityUtils.getCurrentUserAuthorities()
        ).map(t -> new LeadAccessScope(t.getT1(), t.getT2()));
    }

    /** Completes empty for admins; errors with AccessDeniedException otherwise. */
    public Mono<Void> requireAdmin() {
        return currentScope()
            .flatMap(scope -> scope.isAdmin() ? Mono.<Void>empty() : Mono.error(new AccessDeniedException("Administrator access required")));
    }

    /** True when the current user may access the lead. A lead without a preference is admin-only. */
    public Mono<Boolean> canAccessLead(Long leadId) {
        if (leadId == null) {
            return Mono.just(false);
        }
        return currentScope().flatMap(scope -> {
            if (scope.isAdmin()) {
                return Mono.just(true);
            }
            return preferenceRepository
                .findOneByLeadId(leadId)
                .map(pref -> scope.canAccess(pref.getVisaType(), pref.getAssignedTo()))
                .defaultIfEmpty(false);
        });
    }

    /** Completes empty when allowed; errors with AccessDeniedException otherwise. */
    public Mono<Void> checkLeadAccess(Long leadId) {
        return canAccessLead(leadId).flatMap(allowed ->
            allowed ? Mono.<Void>empty() : Mono.error(new AccessDeniedException("You do not have access to this lead"))
        );
    }

    /**
     * Access check for creating a lead's first preference: allowed for admins, for leads the user
     * can already access, and for leads that have no preference yet (a lead a counselor has just
     * created gets its preference in a second request).
     */
    public Mono<Void> checkCanCreatePreference(Long leadId) {
        if (leadId == null) {
            return Mono.error(new AccessDeniedException("A lead is required"));
        }
        return currentScope().flatMap(scope -> {
            if (scope.isAdmin()) {
                return Mono.<Void>empty();
            }
            return preferenceRepository
                .findOneByLeadId(leadId)
                .map(pref -> scope.canAccess(pref.getVisaType(), pref.getAssignedTo()))
                .defaultIfEmpty(true)
                .flatMap(allowed ->
                    allowed ? Mono.<Void>empty() : Mono.error(new AccessDeniedException("You do not have access to this lead"))
                );
        });
    }
}
