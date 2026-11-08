package com.salescrm.service;

import com.salescrm.domain.CrmEvent;
import com.salescrm.domain.CrmNotification;
import com.salescrm.domain.enumeration.LeadStatus;
import com.salescrm.repository.CrmEventRepository;
import com.salescrm.repository.CrmNotificationRepository;
import com.salescrm.service.dto.LeadDetailedDTO;
import com.salescrm.service.util.LeadIdCrypto;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Enterprise Background Scheduled Job for:
 * 1. Due candidate reminders & follow-up callbacks
 * 2. Target Closing Date timeline milestone alerts (25%, 50%, 75%, 100%)
 *
 * STRICT RULES:
 * - Milestone alerts sent ONLY to related staff user assigned to that lead
 * - Milestone alerts sent ONLY when that lead is still OPEN (never for closed leads)
 * - Alerts are deduplicated so each milestone fires exactly once
 */
@Service
public class ReminderNotificationScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ReminderNotificationScheduler.class);
    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy").withZone(DEFAULT_ZONE);
    private static final DateTimeFormatter TIME_12H_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a").withZone(DEFAULT_ZONE);

    private final CrmEventRepository crmEventRepository;
    private final CrmNotificationRepository crmNotificationRepository;
    private final CrmNotificationService crmNotificationService;
    private final FirebasePushNotificationService firebasePushNotificationService;
    private final LeadDetailedService leadDetailedService;

    public ReminderNotificationScheduler(
        CrmEventRepository crmEventRepository,
        CrmNotificationRepository crmNotificationRepository,
        CrmNotificationService crmNotificationService,
        FirebasePushNotificationService firebasePushNotificationService,
        LeadDetailedService leadDetailedService
    ) {
        this.crmEventRepository = crmEventRepository;
        this.crmNotificationRepository = crmNotificationRepository;
        this.crmNotificationService = crmNotificationService;
        this.firebasePushNotificationService = firebasePushNotificationService;
        this.leadDetailedService = leadDetailedService;
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
                LOG.info("Dispatching reminder notification for event: {} (candidate: {}, owner: {})", event.getTitle(), event.getCandidateName(), event.getUserId());
                event.setIsPersisted();
                event.setNotificationSent(Boolean.TRUE);

                String rawCandidate = event.getCandidateName();
                String cleanCandidate = null;
                if (rawCandidate != null && !rawCandidate.isBlank()) {
                    cleanCandidate = rawCandidate.replaceFirst("(?i)^Candidate:\\s*", "").trim();
                }

                String title = event.getTitle();
                String timeStr = event.getEventTime() != null ? TIME_12H_FORMATTER.format(event.getEventTime()) : "today";

                // Format clean reminder message: strictly user-entered data; zero static boilerplate
                String message = "";
                if (event.getDescription() != null && !event.getDescription().isBlank()) {
                    message = event.getDescription().trim();
                } else if (cleanCandidate != null && !cleanCandidate.isBlank() && !cleanCandidate.equalsIgnoreCase(title)) {
                    message = cleanCandidate;
                }

                String targetUserId = (event.getUserId() != null && !event.getUserId().isBlank()) ? event.getUserId() : "admin";
                CrmNotification notification = new CrmNotification();
                notification.setUserId(targetUserId);
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

                // Save event update and notification, then dispatch FCM push strictly to original owner
                return crmEventRepository.save(event)
                    .then(crmNotificationService.createNotification(notification))
                    .doOnSuccess(savedNotif -> {
                        if (firebasePushNotificationService != null && targetUserId != null) {
                            // Send strictly to the original reminder receiver ONLY - NEVER broadcast to all users
                            firebasePushNotificationService.sendPushNotification(
                                targetUserId,
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

    /**
     * Run every 60 seconds to evaluate open candidate leads approaching target closing date milestones (25%, 50%, 75%, 100%).
     *
     * STRICT RULES:
     * 1. Send ONLY to the related staff user assigned to that lead (lead.assignedTo).
     * 2. Send ONLY when that lead is still OPEN (status != WON and status != LOST).
     * 3. Send deduplicated alerts (only once per milestone per staff user).
     */
    @Scheduled(fixedDelay = 60000, initialDelay = 15000)
    public void scanAndDispatchClosingMilestones() {
        if (leadDetailedService == null) return;

        leadDetailedService.findAllDetailed()
            .filter(lead -> {
                // Rule 1: Lead must NOT be closed (open leads only)
                if (lead.getStatus() == LeadStatus.WON || lead.getStatus() == LeadStatus.LOST) {
                    return false;
                }

                // Rule 2: Must have related staff user assigned (never send if unassigned or pool)
                String assigned = lead.getAssignedTo();
                if (assigned == null || assigned.isBlank() ||
                    "General Pool".equalsIgnoreCase(assigned) ||
                    "Unassigned".equalsIgnoreCase(assigned)) {
                    return false;
                }

                // Rule 3: Must have target closing date configured
                String closingDateStr = lead.getClosingDate();
                return closingDateStr != null && !closingDateStr.isBlank();
            })
            .flatMap(this::evaluateLeadClosingMilestones)
            .subscribe(
                success -> {},
                error -> LOG.error("Error evaluating closing milestones: {}", error.getMessage())
            );
    }

    private Mono<Void> evaluateLeadClosingMilestones(LeadDetailedDTO lead) {
        String closingDateStr = lead.getClosingDate();
        Instant end = parseDateSafe(closingDateStr);
        if (end == null) {
            return Mono.empty();
        }

        Instant start = lead.getCreatedTime() != null ? lead.getCreatedTime() : lead.getCreatedAt();
        if (start == null) {
            start = end.minus(java.time.Duration.ofDays(60));
        }

        if (!end.isAfter(start)) {
            return Mono.empty();
        }

        Instant now = Instant.now();
        long totalMillis = end.toEpochMilli() - start.toEpochMilli();
        long elapsedMillis = now.toEpochMilli() - start.toEpochMilli();
        if (totalMillis <= 0) return Mono.empty();

        double progress = ((double) elapsedMillis / (double) totalMillis) * 100.0;
        String assignedStaff = lead.getAssignedTo();
        String encryptedRefId = LeadIdCrypto.encrypt(lead.getId());
        String candidateName = lead.getCustomerName() != null ? lead.getCustomerName() : "Candidate #" + lead.getId();
        String formattedEnd = DATE_FORMATTER.format(end);

        Mono<Void> chain = Mono.empty();

        if (progress >= 100.0) {
            chain = chain.then(checkAndSendMilestone(
                assignedStaff,
                encryptedRefId,
                lead.getId(),
                "CLOSING_MILESTONE_100",
                "⚠️ 100% Target Deadline Reached: " + candidateName,
                "Target visa conversion closing date (" + formattedEnd + ") reached for candidate " + candidateName + ". Please finalize candidate pipeline status."
            ));
        }
        if (progress >= 75.0) {
            chain = chain.then(checkAndSendMilestone(
                assignedStaff,
                encryptedRefId,
                lead.getId(),
                "CLOSING_MILESTONE_75",
                "⏳ 75% Timeline Elapsed: " + candidateName,
                "75% of conversion timeline passed for candidate " + candidateName + ". Target conversion deadline is " + formattedEnd + "."
            ));
        }
        if (progress >= 50.0) {
            chain = chain.then(checkAndSendMilestone(
                assignedStaff,
                encryptedRefId,
                lead.getId(),
                "CLOSING_MILESTONE_50",
                "⏱️ 50% Timeline (Midway): " + candidateName,
                "Candidate " + candidateName + " has reached 50% (midway) of conversion timeline towards " + formattedEnd + "."
            ));
        }
        if (progress >= 25.0) {
            chain = chain.then(checkAndSendMilestone(
                assignedStaff,
                encryptedRefId,
                lead.getId(),
                "CLOSING_MILESTONE_25",
                "📅 25% Timeline Elapsed: " + candidateName,
                "1st Milestone: 25% of conversion timeline elapsed for candidate " + candidateName + " (Target Deadline: " + formattedEnd + ")."
            ));
        }

        return chain;
    }

    private Mono<Void> checkAndSendMilestone(
        String staffUserId,
        String encryptedRefId,
        Long leadId,
        String milestoneType,
        String title,
        String message
    ) {
        return crmNotificationRepository.countByUserIdAndReferenceIdAndType(staffUserId, encryptedRefId, milestoneType)
            .flatMap(count -> {
                if (count != null && count > 0) {
                    // Already sent to this assigned staff member - skip to prevent duplicate notifications
                    return Mono.empty();
                }

                LOG.info("Dispatching milestone {} alert to assigned staff [{}] for candidate lead [{}]",
                    milestoneType, staffUserId, leadId);

                CrmNotification notification = new CrmNotification();
                notification.setUserId(staffUserId);
                notification.setTitle(title);
                notification.setMessage(message);
                notification.setType(milestoneType);
                notification.setIsRead(Boolean.FALSE);
                notification.setReferenceId(encryptedRefId);
                notification.setReferenceType("LEAD");

                Map<String, String> data = new HashMap<>();
                data.put("leadId", String.valueOf(leadId));
                data.put("encryptedLeadId", encryptedRefId);
                data.put("type", milestoneType);
                data.put("title", title);
                data.put("message", message);

                return crmNotificationService.createNotification(notification)
                    .doOnSuccess(saved -> {
                        if (firebasePushNotificationService != null) {
                            // STRICT REQUIREMENT: Send ONLY to the related staff user!
                            firebasePushNotificationService.sendPushNotification(
                                staffUserId,
                                title,
                                message,
                                data
                            );
                        }
                    })
                    .then();
            });
    }

    private Instant parseDateSafe(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            String trimmed = dateStr.trim();
            if (trimmed.length() >= 10 && trimmed.charAt(4) == '-' && trimmed.charAt(7) == '-') {
                LocalDate ld = LocalDate.parse(trimmed.substring(0, 10));
                return ld.atTime(LocalTime.of(18, 0)).atZone(DEFAULT_ZONE).toInstant();
            }
            return Instant.parse(trimmed);
        } catch (Exception e) {
            try {
                LocalDate ld = LocalDate.parse(dateStr.substring(0, 10));
                return ld.atTime(LocalTime.of(18, 0)).atZone(DEFAULT_ZONE).toInstant();
            } catch (Exception ignored) {
                return null;
            }
        }
    }
}
