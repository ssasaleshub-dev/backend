package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salescrm.domain.Lead;
import com.salescrm.service.FacebookLeadService;
import com.salescrm.service.FacebookWebhookVerificationService;
import com.salescrm.service.dto.facebook.FacebookWebhookPayloadDTO;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

@ExtendWith(MockitoExtension.class)
class FacebookWebhookControllerTest {

    @Mock
    private FacebookWebhookVerificationService verificationService;

    @Mock
    private FacebookLeadService facebookLeadService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private FacebookWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new FacebookWebhookController(verificationService, facebookLeadService, objectMapper);
    }

    @Test
    void shouldReturnChallengeWhenGetVerificationSucceeds() {
        when(verificationService.verifySubscriptionToken("subscribe", "my_secret_token")).thenReturn(true);

        ResponseEntity<String> response = controller.verifyWebhook("subscribe", "my_secret_token", "challenge_123456").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("challenge_123456");
    }

    @Test
    void shouldReturnForbiddenWhenGetVerificationFails() {
        when(verificationService.verifySubscriptionToken("subscribe", "wrong_token")).thenReturn(false);

        ResponseEntity<String> response = controller.verifyWebhook("subscribe", "wrong_token", "challenge_123456").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).contains("mismatch");
    }

    @Test
    void shouldAcknowledgePostWebhookWith200WhenSignatureIsValid() {
        String jsonPayload = "{\"object\":\"page\",\"entry\":[{\"id\":\"123\",\"changes\":[{\"field\":\"leadgen\",\"value\":{\"leadgen_id\":\"999\"}}]}]}";
        byte[] payloadBytes = jsonPayload.getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=validdigest123";

        when(verificationService.verifySignature(eq(signature), any(byte[].class))).thenReturn(true);
        when(facebookLeadService.processWebhookPayload(any(FacebookWebhookPayloadDTO.class))).thenReturn(Flux.just(new Lead()));

        ResponseEntity<String> response = controller.handleWebhookEvent(signature, payloadBytes).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("EVENT_RECEIVED");

        verify(facebookLeadService).processWebhookPayload(any(FacebookWebhookPayloadDTO.class));
    }

    @Test
    void shouldRejectPostWebhookWith403WhenSignatureIsInvalid() {
        byte[] payloadBytes = "{}".getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=invaliddigest";

        when(verificationService.verifySignature(eq(signature), any(byte[].class))).thenReturn(false);

        ResponseEntity<String> response = controller.handleWebhookEvent(signature, payloadBytes).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isEqualTo("Invalid signature");

        verify(facebookLeadService, never()).processWebhookPayload(any());
    }
}
