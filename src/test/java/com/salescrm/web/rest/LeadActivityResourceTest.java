package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.salescrm.domain.LeadActivity;
import com.salescrm.service.LeadActivityService;
import com.salescrm.service.dto.LeadActivityDTO;
import java.time.Instant;
import java.util.List;
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
class LeadActivityResourceTest {

    @Mock
    private LeadActivityService leadActivityService;

    private LeadActivityResource resource;

    @BeforeEach
    void setUp() {
        resource = new LeadActivityResource(leadActivityService);
    }

    @Test
    void shouldGetLeadActivities() {
        LeadActivity activity1 = new LeadActivity();
        activity1.setId(1L);
        activity1.setLeadId(100L);
        activity1.setActionType("STATUS_CHANGE");
        activity1.setSummary("Status changed from NEW to CONTACTED");
        activity1.setUserName("Counselor Priya");

        when(leadActivityService.getActivitiesForLead(100L)).thenReturn(Flux.just(activity1));

        List<LeadActivity> result = resource.getLeadActivities(100L).collectList().block();

        assertThat(result).isNotNull().hasSize(1);
        assertThat(result.get(0).getActionType()).isEqualTo("STATUS_CHANGE");
        assertThat(result.get(0).getUserName()).isEqualTo("Counselor Priya");
        verify(leadActivityService).getActivitiesForLead(100L);
    }

    @Test
    void shouldCreateLeadActivity() {
        LeadActivityDTO dto = new LeadActivityDTO();
        dto.setActionType("NOTE_ADDED");
        dto.setSummary("Followed up regarding airport ground staff course");
        dto.setDetails("Candidate interested in batch starting next month");
        dto.setUserName("Counselor Priya");

        LeadActivity saved = new LeadActivity();
        saved.setId(10L);
        saved.setLeadId(100L);
        saved.setActionType("NOTE_ADDED");
        saved.setSummary(dto.getSummary());
        saved.setDetails(dto.getDetails());
        saved.setUserName("Counselor Priya");
        saved.setCreatedAt(Instant.now());

        when(leadActivityService.logActivity(any(LeadActivityDTO.class))).thenReturn(Mono.just(saved));

        ResponseEntity<LeadActivity> response = resource.createLeadActivity(100L, dto).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(10L);
        assertThat(response.getBody().getLeadId()).isEqualTo(100L);
        assertThat(response.getBody().getSummary()).isEqualTo(dto.getSummary());
        verify(leadActivityService).logActivity(any(LeadActivityDTO.class));
    }
}
