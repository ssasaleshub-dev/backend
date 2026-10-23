package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.salescrm.domain.CrmNotification;
import com.salescrm.service.CrmNotificationService;
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
class NotificationResourceTest {

    @Mock
    private CrmNotificationService crmNotificationService;

    private NotificationResource notificationResource;

    @BeforeEach
    void setUp() {
        notificationResource = new NotificationResource(crmNotificationService);
    }

    @Test
    void shouldGetNotifications() {
        CrmNotification notification = new CrmNotification();
        notification.setId(UUID.randomUUID());
        notification.setUserId("user1");
        notification.setTitle("New lead assigned");
        notification.setMessage("Lead Jane Doe has been assigned to you");
        notification.setType("LEAD_ASSIGNED");

        when(crmNotificationService.getNotifications(eq("user1"), eq(false))).thenReturn(Flux.just(notification));

        ResponseEntity<List<CrmNotification>> response = notificationResource.getNotifications("user1", false).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("New lead assigned");
    }

    @Test
    void shouldGetUnreadCount() {
        when(crmNotificationService.getUnreadCount(eq("user1"))).thenReturn(Mono.just(4L));

        ResponseEntity<Map<String, Object>> response = notificationResource.getUnreadCount("user1").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("unreadCount")).isEqualTo(4L);
        assertThat(response.getBody().get("count")).isEqualTo(4L);
    }

    @Test
    void shouldMarkAsRead() {
        UUID id = UUID.randomUUID();
        CrmNotification notification = new CrmNotification();
        notification.setId(id);
        notification.setIsRead(true);

        when(crmNotificationService.markAsRead(eq(id))).thenReturn(Mono.just(notification));

        ResponseEntity<CrmNotification> response = notificationResource.markAsRead(id).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getIsRead()).isTrue();
    }

    @Test
    void shouldMarkAllAsRead() {
        when(crmNotificationService.markAllAsRead(eq("user1"))).thenReturn(Mono.just(5L));

        ResponseEntity<Map<String, Object>> response = notificationResource.markAllAsRead("user1").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("updatedCount")).isEqualTo(5L);
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    void shouldDeleteNotification() {
        UUID id = UUID.randomUUID();
        when(crmNotificationService.deleteNotification(eq(id))).thenReturn(Mono.empty());

        ResponseEntity<Void> response = notificationResource.deleteNotification(id).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldClearNotifications() {
        when(crmNotificationService.clearAllNotifications(eq("user1"))).thenReturn(Mono.just(10L));

        ResponseEntity<Void> response = notificationResource.clearNotifications("user1").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
