package com.salescrm.service;

import com.salescrm.domain.CrmEvent;
import com.salescrm.domain.CrmNotification;
import com.salescrm.repository.CrmEventRepository;
import com.salescrm.service.util.LeadIdCrypto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Enterprise Background Scheduled Job for checking and delivering due candidate reminders.
 * Automatically dispatches:
 * 1. Persistent in-app CrmNotification in database for Web App & Android
 * 2. Real-time Firebase Cloud Messaging (FCM) push notification to Android devices
 */
@Service
public class ReminderNotificationScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ReminderNotificationScheduler.class);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");

    private final CrmEventRepository crmEventRepository;
    private final CrmNotificationService crmNotificationService;
    private final FirebasePushNotificationService firebasePushNotificationService;

    public ReminderNotificationScheduler(
        CrmEventRepository crmEventRepository,
        CrmNotificationService crmNotificationService,
        FirebasePushNotificationService firebasePushNotificationService
    ) {
        this.crmEventRepository = crmEventRepository;
        this.crmNotificationService = crmNotificationService;
        this.firebasePushNotificationService = firebasePushNotificationService;
    }

    /**
     * Run every 30 seconds to evaluate pending reminders.
     */
    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void scanAndDispatchReminders() {
        LocalDate today = LocalDate.now(DEFAULT_ZONE);
        LocalDateTime now = LocalDateTime.now(DEFAULT_ZONE);

        crmEventRepository.findPendingReminders(today)
            .filter(event -> {
                if (Boolean.TRUE.equals(event.getIsCompleted()) || Boolean.TRUE.equals(event.getNotificationSent())) {
                    return false;
                }
                LocalDate eventDate = event.getEventDate();
                if (eventDate == null) return false;
                LocalTime eventTime = event.getEventTime() != null ? event.getEventTime() : LocalTime.of(9, 0);
                LocalDateTime eventDateTime = LocalDateTime.of(eventDate, eventTime);

                int remindBefore = event.getRemindBeforeMinutes() != null ? event.getRemindBeforeMinutes() : 0;
                LocalDateTime triggerTime = eventDateTime.minusMinutes(remindBefore);

                // If triggerTime <= now and event is not older than 7 days
                return !now.isBefore(triggerTime) && !now.isAfter(eventDateTime.plusDays(7));
            })
            .flatMap(event -> {
                LOG.info("Dispatching reminder notification for event: {} (candidate: {})", event.getTitle(), event.getCandidateName());
                event.setIsPersisted();
                event.setNotificationSent(Boolean.TRUE);

                String candidateName = event.getCandidateName() != null ? event.getCandidateName() : "Candidate";
                String title = "🔔 Reminder: " + event.getTitle();
                String message = "Reminder for candidate " + candidateName + ": " +
                    (event.getDescription() != null && !event.getDescription().isBlank() ? event.getDescription() : event.getTitle()) +
                    " (Scheduled for " + (event.getEventTime() != null ? event.getEventTime().toString() : "today") + ")";

                CrmNotification notification = new CrmNotification();
                notification.setUserId(event.getUserId() != null ? event.getUserId() : "admin");
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setType("REMINDER");
                notification.setIsRead(Boolean.FALSE);
                if (event.getCandidateId() != null) {
                    notification.setReferenceId(LeadIdCrypto.encrypt(event.getCandidateId()));
                    notification.setReferenceType("LEAD");
                } else {
                    notification.setReferenceId(event.getId() != null ? event.getId().toString() : null);
                    notification.setReferenceType("EVENT");
                }

                Map<String, String> data = new HashMap<>();
                if (event.getCandidateId() != null) {
                    data.put("leadId", String.valueOf(event.getCandidateId()));
                    data.put("encryptedLeadId", LeadIdCrypto.encrypt(event.getCandidateId()));
                }
                data.put("type", "REMINDER");
                data.put("title", title);
                data.put("message", message);

                // Save event update and notification, then dispatch FCM push
                return crmEventRepository.save(event)
                    .then(crmNotificationService.createNotification(notification))
                    .doOnSuccess(savedNotif -> {
                        if (firebasePushNotificationService != null) {
                            firebasePushNotificationService.sendPushNotification(
                                notification.getUserId(),
                                title,
                                message,
                                data
                            );
                            // Also broadcast to other connected staff tokens if any
                            firebasePushNotificationService.sendPushNotificationToAll(
                                title,
                                message,
                                data
                            );
                        }
                    });
            })
            .subscribe(
                success -> LOG.debug("Processed reminder notification"),
                error -> LOG.error("Error evaluating reminder notifications: {}", error.getMessage())
            );
    }
}
