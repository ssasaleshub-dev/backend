package com.salescrm.web.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.salescrm.domain.CrmNotification;
import com.salescrm.domain.User;
import com.salescrm.repository.CrmNotificationRepository;
import com.salescrm.repository.LeadPreferenceRepository;
import com.salescrm.repository.UserRepository;
import com.salescrm.security.AuthoritiesConstants;
import com.salescrm.service.CrmNotificationService;
import com.salescrm.service.LeadAccessService;
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
class NotificationResourceTest {

    @Mock
    private CrmNotificationService crmNotificationService;

    @Mock
    private CrmNotificationRepository crmNotificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeadPreferenceRepository leadPreferenceRepository;

    private NotificationResource notificationResource;

    @BeforeEach
    void setUp() {
        notificationResource = new NotificationResource(
            crmNotificationService,
            null,
            userRepository,
            crmNotificationRepository,
            new LeadAccessService(leadPreferenceRepository)
        );
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

    private static <T> Mono<T> asStaff(Mono<T> mono, String login) {
        return as(mono, login, AuthoritiesConstants.WORK_VISA);
    }

    private static CrmNotification notification(UUID id, String owner) {
        CrmNotification n = new CrmNotification();
        n.setId(id);
        n.setUserId(owner);
        n.setTitle("New lead assigned");
        return n;
    }

    @Test
    void shouldGetNotifications() {
        CrmNotification n = notification(UUID.randomUUID(), "user1");
        n.setMessage("Lead Jane Doe has been assigned to you");
        n.setType("LEAD_ASSIGNED");

        when(crmNotificationService.getNotifications(eq("user1"), eq(false))).thenReturn(Flux.just(n));

        ResponseEntity<List<CrmNotification>> response = asAdmin(notificationResource.getNotifications("user1", false)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).getTitle()).isEqualTo("New lead assigned");
    }

    @Test
    void staffRequestingAnotherInboxGetsTheirOwn() {
        when(crmNotificationService.getNotifications(eq("user1"), eq(false))).thenReturn(Flux.empty());

        asStaff(notificationResource.getNotifications("someone-else", false), "user1").block();

        verify(crmNotificationService).getNotifications(eq("user1"), eq(false));
        verify(crmNotificationService, never()).getNotifications(eq("someone-else"), any());
    }

    @Test
    void shouldGetUnreadCount() {
        when(crmNotificationService.getUnreadCount(eq("user1"))).thenReturn(Mono.just(4L));

        ResponseEntity<Map<String, Object>> response = asStaff(notificationResource.getUnreadCount("user1"), "user1").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("unreadCount")).isEqualTo(4L);
        assertThat(response.getBody().get("count")).isEqualTo(4L);
    }

    @Test
    void shouldMarkAsRead() {
        UUID id = UUID.randomUUID();
        CrmNotification read = notification(id, "user1");
        read.setIsRead(true);

        when(crmNotificationRepository.findById(id)).thenReturn(Mono.just(notification(id, "user1")));
        when(userRepository.findOneByLogin("user1")).thenReturn(Mono.empty());
        when(crmNotificationService.markAsRead(eq(id))).thenReturn(Mono.just(read));

        ResponseEntity<CrmNotification> response = asStaff(notificationResource.markAsRead(id), "user1").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getIsRead()).isTrue();
    }

    @Test
    void ownerMatchedByNumericUserId() {
        UUID id = UUID.randomUUID();
        User me = new User();
        me.setId(7L);
        me.setLogin("user1");

        when(crmNotificationRepository.findById(id)).thenReturn(Mono.just(notification(id, "7")));
        when(userRepository.findOneByLogin("user1")).thenReturn(Mono.just(me));
        when(crmNotificationService.deleteNotification(eq(id))).thenReturn(Mono.empty());

        ResponseEntity<Void> response = asStaff(notificationResource.deleteNotification(id), "user1").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void staffCannotDeleteAnotherUsersNotification() {
        UUID id = UUID.randomUUID();
        when(crmNotificationRepository.findById(id)).thenReturn(Mono.just(notification(id, "user2")));
        when(userRepository.findOneByLogin("user1")).thenReturn(Mono.empty());

        assertThatThrownBy(() -> asStaff(notificationResource.deleteNotification(id), "user1").block()).isInstanceOf(AccessDeniedException.class);
        verify(crmNotificationService, never()).deleteNotification(any());
    }

    @Test
    void shouldMarkAllAsRead() {
        when(crmNotificationService.markAllAsRead(eq("user1"))).thenReturn(Mono.just(5L));

        ResponseEntity<Map<String, Object>> response = asStaff(notificationResource.markAllAsRead("user1"), "user1").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("updatedCount")).isEqualTo(5L);
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    void shouldDeleteNotificationAsAdmin() {
        UUID id = UUID.randomUUID();
        when(crmNotificationService.deleteNotification(eq(id))).thenReturn(Mono.empty());

        ResponseEntity<Void> response = asAdmin(notificationResource.deleteNotification(id)).block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void shouldClearNotifications() {
        when(crmNotificationService.clearAllNotifications(eq("user1"))).thenReturn(Mono.just(10L));

        ResponseEntity<Void> response = asStaff(notificationResource.clearNotifications("user1"), "user1").block();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
