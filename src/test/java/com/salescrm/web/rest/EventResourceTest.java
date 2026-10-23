package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.salescrm.domain.CrmEvent;
import com.salescrm.service.CrmEventService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class EventResourceTest {

    @Mock
    private CrmEventService crmEventService;

    private EventResource eventResource;

    @BeforeEach
    void setUp() {
        eventResource = new EventResource(crmEventService);
    }

    @Test
    void shouldGetEventsByUserId() {
        CrmEvent event = new CrmEvent();
        event.setId(UUID.randomUUID());
        event.setUserId("counselor1");
        event.setTitle("Candidate Follow-up");
        event.setEventDate(LocalDate.now());

        when(crmEventService.getEvents(eq("counselor1"), eq(null))).thenReturn(Flux.just(event));

        ResponseEntity<List<CrmEvent>> response = eventResource.getEvents("counselor1", null).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("Candidate Follow-up");
    }

    @Test
    void shouldCreateEvent() {
        UUID eventId = UUID.randomUUID();
        CrmEvent input = new CrmEvent();
        input.setUserId("counselor1");
        input.setTitle("Document Collection");
        input.setEventDate(LocalDate.now());
        input.setEventTime(LocalTime.of(14, 30));

        CrmEvent created = new CrmEvent();
        created.setId(eventId);
        created.setUserId("counselor1");
        created.setTitle("Document Collection");
        created.setEventDate(LocalDate.now());
        created.setEventTime(LocalTime.of(14, 30));

        when(crmEventService.createEvent(any(CrmEvent.class))).thenReturn(Mono.just(created));

        ResponseEntity<CrmEvent> response = eventResource.createEvent(input).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(eventId);
    }

    @Test
    void shouldToggleComplete() {
        UUID eventId = UUID.randomUUID();
        CrmEvent completedEvent = new CrmEvent();
        completedEvent.setId(eventId);
        completedEvent.setIsCompleted(true);

        when(crmEventService.toggleComplete(eq(eventId), eq(true))).thenReturn(Mono.just(completedEvent));

        ResponseEntity<CrmEvent> response = eventResource.toggleComplete(eventId, true, null).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsCompleted()).isTrue();
    }

    @Test
    void shouldToggleCompleteFromBody() {
        UUID eventId = UUID.randomUUID();
        CrmEvent completedEvent = new CrmEvent();
        completedEvent.setId(eventId);
        completedEvent.setIsCompleted(true);

        when(crmEventService.toggleComplete(eq(eventId), eq(true))).thenReturn(Mono.just(completedEvent));

        ResponseEntity<CrmEvent> response = eventResource.toggleComplete(eventId, null, Map.of("isCompleted", true)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsCompleted()).isTrue();
    }

    @Test
    void shouldDeleteEvent() {
        UUID eventId = UUID.randomUUID();
        when(crmEventService.deleteEvent(eq(eventId))).thenReturn(Mono.empty());

        ResponseEntity<Void> response = eventResource.deleteEvent(eventId).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
