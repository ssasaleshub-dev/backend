package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.salescrm.domain.CrmEvent;
import com.salescrm.domain.LeadPreference;
import com.salescrm.repository.LeadPreferenceRepository;
import com.salescrm.security.AuthoritiesConstants;
import com.salescrm.service.CrmEventService;
import com.salescrm.service.LeadAccessService;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class EventResourceTest {

    @Mock
    private CrmEventService crmEventService;

    @Mock
    private LeadPreferenceRepository leadPreferenceRepository;

    private EventResource eventResource;

    @BeforeEach
    void setUp() {
        eventResource = new EventResource(crmEventService, null, null, new LeadAccessService(leadPreferenceRepository));
    }

    private static <T> Mono<T> as(Mono<T> mono, String login, String... authorities) {
        return mono.contextWrite(
            ReactiveSecurityContextHolder.withAuthentication(
                new UsernamePasswordAuthenticationToken(login, "n/a", AuthorityUtils.createAuthorityList(authorities))
            )
        );
    }

    private static <T> Mono<T> asAdmin(Mono<T> mono) {
        return as(mono, "admin", AuthoritiesConstants.ADMIN);
    }

    private static CrmEvent event(UUID id, String owner, Long candidateId) {
        CrmEvent e = new CrmEvent();
        e.setId(id);
        e.setUserId(owner);
        e.setCandidateId(candidateId);
        e.setTitle("Follow-up");
        e.setEventDate(LocalDate.now());
        return e;
    }

    @Test
    void shouldGetEventsByUserId() {
        CrmEvent event = event(UUID.randomUUID(), "counselor1", null);
        event.setTitle("Candidate Follow-up");

        when(crmEventService.getEvents(eq("counselor1"), eq(null))).thenReturn(Flux.just(event));

        ResponseEntity<List<CrmEvent>> response = asAdmin(eventResource.getEvents("counselor1", null)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("Candidate Follow-up");
    }

    @Test
    void staffCannotReadAnotherUsersEvents() {
        assertThatThrownBy(() -> as(eventResource.getEvents("counselor2", null), "counselor1", AuthoritiesConstants.WORK_VISA).block()).isInstanceOf(AccessDeniedException.class);
        verify(crmEventService, never()).getEvents(any(), any());
    }

    @Test
    void staffWithoutUserIdGetsOwnEvents() {
        when(crmEventService.getEvents(eq("counselor1"), eq(null))).thenReturn(Flux.just(event(UUID.randomUUID(), "counselor1", null)));

        ResponseEntity<List<CrmEvent>> response = as(eventResource.getEvents(null, null), "counselor1", AuthoritiesConstants.WORK_VISA).block();

        assertThat(response).isNotNull();
        assertThat(response.getBody()).hasSize(1);
    }

    @Test
    void shouldCreateEvent() {
        UUID eventId = UUID.randomUUID();
        CrmEvent input = event(null, "counselor1", null);
        input.setTitle("Document Collection");
        input.setEventTime(LocalTime.of(14, 30));

        CrmEvent created = event(eventId, "counselor1", null);
        when(crmEventService.createEvent(any(CrmEvent.class))).thenReturn(Mono.just(created));

        ResponseEntity<CrmEvent> response = asAdmin(eventResource.createEvent(input)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getId()).isEqualTo(eventId);
    }

    @Test
    void staffCreatingUnlinkedEventForSomeoneElseOwnsItThemselves() {
        CrmEvent input = event(null, "counselor2", null);
        when(crmEventService.createEvent(any(CrmEvent.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        ResponseEntity<CrmEvent> response = as(eventResource.createEvent(input), "counselor1", AuthoritiesConstants.WORK_VISA).block();

        assertThat(response).isNotNull();
        assertThat(response.getBody().getUserId()).isEqualTo("counselor1");
    }

    @Test
    void shouldToggleComplete() {
        UUID eventId = UUID.randomUUID();
        CrmEvent completedEvent = event(eventId, "counselor1", null);
        completedEvent.setIsCompleted(true);

        when(crmEventService.getEventById(eq(eventId))).thenReturn(Mono.just(event(eventId, "counselor1", null)));
        when(crmEventService.toggleComplete(eq(eventId), eq(true))).thenReturn(Mono.just(completedEvent));

        ResponseEntity<CrmEvent> response = asAdmin(eventResource.toggleComplete(eventId, true, null)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIsCompleted()).isTrue();
    }

    @Test
    void shouldToggleCompleteFromBody() {
        UUID eventId = UUID.randomUUID();
        CrmEvent completedEvent = event(eventId, "counselor1", null);
        completedEvent.setIsCompleted(true);

        when(crmEventService.getEventById(eq(eventId))).thenReturn(Mono.just(event(eventId, "counselor1", null)));
        when(crmEventService.toggleComplete(eq(eventId), eq(true))).thenReturn(Mono.just(completedEvent));

        ResponseEntity<CrmEvent> response = as(
            eventResource.toggleComplete(eventId, null, Map.of("isCompleted", true)),
            "counselor1",
            AuthoritiesConstants.WORK_VISA
        ).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIsCompleted()).isTrue();
    }

    @Test
    void shouldDeleteEvent() {
        UUID eventId = UUID.randomUUID();
        when(crmEventService.getEventById(eq(eventId))).thenReturn(Mono.just(event(eventId, "counselor1", null)));
        when(crmEventService.deleteEvent(eq(eventId))).thenReturn(Mono.empty());

        ResponseEntity<Void> response = asAdmin(eventResource.deleteEvent(eventId)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void staffCannotDeleteAnotherUsersUnlinkedEvent() {
        UUID eventId = UUID.randomUUID();
        when(crmEventService.getEventById(eq(eventId))).thenReturn(Mono.just(event(eventId, "counselor2", null)));

        assertThatThrownBy(() -> as(eventResource.deleteEvent(eventId), "counselor1", AuthoritiesConstants.WORK_VISA).block()).isInstanceOf(AccessDeniedException.class);
        verify(crmEventService, never()).deleteEvent(any());
    }

    @Test
    void staffCanManageCandidateReminderForLeadInTheirDesk() {
        UUID eventId = UUID.randomUUID();
        LeadPreference pref = new LeadPreference();
        pref.setVisaType("WORK_VISA");
        when(crmEventService.getEventById(eq(eventId))).thenReturn(Mono.just(event(eventId, "counselor2", 42L)));
        when(leadPreferenceRepository.findOneByLeadId(42L)).thenReturn(Mono.just(pref));
        when(crmEventService.deleteEvent(eq(eventId))).thenReturn(Mono.empty());

        ResponseEntity<Void> response = as(eventResource.deleteEvent(eventId), "counselor1", AuthoritiesConstants.WORK_VISA).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
