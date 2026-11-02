package com.salescrm.repository;

import com.salescrm.domain.LeadActivity;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

/**
 * Spring Data R2DBC repository for the {@link LeadActivity} entity.
 */
@Repository
public interface LeadActivityRepository extends R2dbcRepository<LeadActivity, Long> {

    Flux<LeadActivity> findAllByLeadIdOrderByCreatedAtDesc(Long leadId);
}

