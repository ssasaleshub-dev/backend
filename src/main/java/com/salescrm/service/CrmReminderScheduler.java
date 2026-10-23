package com.salescrm.service;

import com.salescrm.domain.CrmNotification;
import com.salescrm.repository.CrmEventRepository;
import com.salescrm.repository.CrmNotificationRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 30-Second Scheduled Worker:
 * Checks due reminders and automatically dispatches internal notification records into crm_notifications.
 */
@Service
public class CrmReminderScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(CrmReminderScheduler.class);

    private final CrmEventRepository crmEventRepository;
    private final CrmNotificationRepository crmNotificationRepository;

    public CrmReminderScheduler(CrmEventRepository crmEventRepository, CrmNotificationRepository crmNotificationRepository) {
        this.crmEventRepository = crmEventRepository;
        this.crmNotificationRepository = crmNotificationRepository;
    }

    @Scheduled(fixedDelay = 30000, initialDelay = 10000)
    public void processDueReminders() {
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        crmEventRepository
            .findPendingReminders(today)
            .filter(event -> {
                LocalTime time = event.getEventTime() != null ? event.getEventTime() : LocalTime.of(9, 0);
                int remindBefore = event.getRemindBeforeMinutes() != null ? event.getRemindBeforeMinutes() : 15;
                LocalDateTime eventDateTime = LocalDateTime.of(event.getEventDate(), time);
                LocalDateTime triggerTime = eventDateTime.minusMinutes(remindBefore);
                return !now.isBefore(triggerTime);
            })
            .flatMap(event -> {
                CrmNotification notification = new CrmNotification();
                notification.setId(UUID.randomUUID());
                notification.setUserId(event.getUserId());
                notification.setTitle("Reminder: " + event.getTitle());

                String message = (event.getDescription() != null && !event.getDescription().isBlank())
                    ? event.getDescription()
                    : "Scheduled callback / event for " + (event.getCandidateName() != null ? event.getCandidateName() : "lead");
                notification.setMessage(message);
                notification.setType(event.getEventType() != null ? event.getEventType() : "REMINDER");
                notification.setReferenceId(event.getId() != null ? event.getId().toString() : null);
                notification.setReferenceType("EVENT");
                notification.setIsRead(false);
                notification.setCreatedAt(Instant.now());

                event.setIsPersisted();
                event.setNotificationSent(true);
                event.setUpdatedAt(Instant.now());

                return crmNotificationRepository
                    .save(notification)
                    .then(crmEventRepository.save(event))
                    .doOnSuccess(saved -> LOG.info("Generated reminder alert notification for event ID: {}", event.getId()));
            })
            .doOnError(err -> LOG.error("Error occurred while processing due reminders: {}", err.getMessage(), err))
            .subscribe();
    }
}
