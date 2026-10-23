package com.salescrm.repository;

import com.salescrm.domain.CrmEvent;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CrmEventRepository extends ReactiveCrudRepository<CrmEvent, UUID> {

    Flux<CrmEvent> findAllByUserIdOrderByEventDateAscEventTimeAsc(String userId);

    Flux<CrmEvent> findAllByOrderByEventDateAscEventTimeAsc();

    Flux<CrmEvent> findAllByCandidateId(Long candidateId);

    @Query("SELECT * FROM crm_events_reminders WHERE is_completed = false AND notification_sent = false AND event_date <= :date")
    Flux<CrmEvent> findPendingReminders(LocalDate date);
}
