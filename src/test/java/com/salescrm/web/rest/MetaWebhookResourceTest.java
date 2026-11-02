package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.salescrm.service.MetaGraphApiService;
import com.salescrm.service.dto.meta.MetaWebhookPayload;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class MetaWebhookResourceTest {

    @Mock
    private MetaGraphApiService metaGraphApiService;

    private MetaWebhookResource resource;

    @BeforeEach
    void setUp() throws Exception {
        resource = new MetaWebhookResource(metaGraphApiService);
        Field verifyTokenField = MetaWebhookResource.class.getDeclaredField("verifyToken");
        verifyTokenField.setAccessible(true);
        verifyTokenField.set(resource, "TEST_VERIFY_TOKEN_123");
    }

    @Test
    void shouldVerifyWebhookSuccessfully() {
        ResponseEntity<String> response = resource.verifyWebhook("subscribe", "TEST_VERIFY_TOKEN_123", "challenge_9999");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("challenge_9999");
    }

    @Test
    void shouldRejectWebhookWithInvalidToken() {
        ResponseEntity<String> response = resource.verifyWebhook("subscribe", "WRONG_TOKEN", "challenge_9999");

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldHandleLeadgenEvent() {
        MetaWebhookPayload payload = new MetaWebhookPayload();
        payload.setObject("page");
        payload.setEntry(Collections.emptyList());

        when(metaGraphApiService.processWebhookPayload(any(MetaWebhookPayload.class))).thenReturn(Mono.empty());

        ResponseEntity<String> response = resource.handleLeadgenEvent("sha256=mock", payload).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("EVENT_RECEIVED");
        verify(metaGraphApiService).processWebhookPayload(any(MetaWebhookPayload.class));
    }

    @Test
    void shouldSyncHistoricalLeadsSuccessfully() {
        when(metaGraphApiService.syncHistoricalFormLeads("FORM_123")).thenReturn(Mono.just(42));

        ResponseEntity<Map<String, Object>> response = resource.syncHistoricalLeads("FORM_123").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        assertThat(response.getBody().get("syncedCount")).isEqualTo(42);
    }
}
