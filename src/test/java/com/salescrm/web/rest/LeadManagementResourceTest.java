package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.service.LeadDetailedService;
import com.salescrm.service.dto.LeadDetailedDTO;
import com.salescrm.service.dto.LeadStatusUpdateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class LeadManagementResourceTest {

    @Mock
    private LeadDetailedService leadDetailedService;

    private LeadManagementResource leadManagementResource;

    @BeforeEach
    void setUp() {
        leadManagementResource = new LeadManagementResource(leadDetailedService);
    }

    @Test
    void shouldUpdateStatusToProposalSent() {
        LeadStatusUpdateDTO dto = new LeadStatusUpdateDTO(LeadStatus.PROPOSAL_SENT, "Documents requested");
        LeadDetailedDTO resultDto = new LeadDetailedDTO();
        resultDto.setId(958L);
        resultDto.setStatus(LeadStatus.PROPOSAL_SENT);

        when(leadDetailedService.updateStatus(eq(958L), eq(LeadStatus.PROPOSAL_SENT), eq("Documents requested")))
            .thenReturn(Mono.just(resultDto));

        ResponseEntity<LeadDetailedDTO> response = leadManagementResource.updateLeadStatus(958L, dto).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(LeadStatus.PROPOSAL_SENT);
    }

    @Test
    void shouldUpdateStatusToNegotiation() {
        LeadStatusUpdateDTO dto = new LeadStatusUpdateDTO(LeadStatus.NEGOTIATION, "Processing application");
        LeadDetailedDTO resultDto = new LeadDetailedDTO();
        resultDto.setId(958L);
        resultDto.setStatus(LeadStatus.NEGOTIATION);

        when(leadDetailedService.updateStatus(eq(958L), eq(LeadStatus.NEGOTIATION), eq("Processing application")))
            .thenReturn(Mono.just(resultDto));

        ResponseEntity<LeadDetailedDTO> response = leadManagementResource.updateLeadStatus(958L, dto).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(LeadStatus.NEGOTIATION);
    }

    @Test
    void shouldUpdateStatusToWon() {
        LeadStatusUpdateDTO dto = new LeadStatusUpdateDTO(LeadStatus.WON, "Visa approved");
        LeadDetailedDTO resultDto = new LeadDetailedDTO();
        resultDto.setId(958L);
        resultDto.setStatus(LeadStatus.WON);

        when(leadDetailedService.updateStatus(eq(958L), eq(LeadStatus.WON), eq("Visa approved")))
            .thenReturn(Mono.just(resultDto));

        ResponseEntity<LeadDetailedDTO> response = leadManagementResource.updateLeadStatus(958L, dto).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(LeadStatus.WON);
    }
}
