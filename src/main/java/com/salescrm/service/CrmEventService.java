package com.salescrm.service;

import com.salescrm.domain.CrmEvent;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CrmEventService {

    Flux<CrmEvent> getEvents(String userId, Long candidateId);

    Mono<CrmEvent> getEventById(UUID id);

    Mono<CrmEvent> createEvent(CrmEvent event);

    Mono<CrmEvent> updateEvent(UUID id, CrmEvent event);

    Mono<CrmEvent> toggleComplete(UUID id, Boolean isCompleted);

    Mono<Void> deleteEvent(UUID id);
}
