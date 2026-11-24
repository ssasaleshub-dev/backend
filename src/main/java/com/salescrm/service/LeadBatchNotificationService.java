package com.salescrm.service;

import com.salescrm.domain.CrmNotification;
import com.salescrm.repository.UserRepository;
import com.salescrm.service.dto.LeadBatchItemDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * MNC-standard Lead Notification Batching Service.
 *
 * Groups incoming leads into batches of 10 before dispatching:
 * 1. An HTML table digest email to all CRM Admins.
 * 2. A grouped push notification via FCM.
 * 3. A consolidated in-app CrmNotification in the database.
 *
 * Also includes a periodic flush so that pending leads (< 10) are still
 * safely reported after an inactivity window.
 */
@Service
public class LeadBatchNotificationService {

    private static final Logger LOG = LoggerFactory.getLogger(LeadBatchNotificationService.class);
    private static final int BATCH_SIZE = 10;

    private final Queue<LeadBatchItemDTO> leadQueue = new ConcurrentLinkedQueue<>();

    private final CrmEmailService crmEmailService;
    private final FirebasePushNotificationService pushNotificationService;
    private final CrmNotificationService crmNotificationService;
    private final UserRepository userRepository;

    public LeadBatchNotificationService(
        @Autowired(required = false) CrmEmailService crmEmailService,
        @Autowired(required = false) FirebasePushNotificationService pushNotificationService,
        @Autowired(required = false) CrmNotificationService crmNotificationService,
        @Autowired(required = false) UserRepository userRepository
    ) {
        this.crmEmailService = crmEmailService;
        this.pushNotificationService = pushNotificationService;
        this.crmNotificationService = crmNotificationService;
        this.userRepository = userRepository;
    }

    /**
     * Enqueue a new incoming lead.
     * When 10 leads accumulate, immediately flushes and sends the batch notification.
     */
    public synchronized void queueLead(LeadBatchItemDTO item) {
        if (item == null) {
            return;
        }
        leadQueue.offer(item);
        LOG.info("[LeadBatch] Queued lead: '{}' (Current batch buffer: {}/{})",
            item.getName(), leadQueue.size(), BATCH_SIZE);

        if (leadQueue.size() >= BATCH_SIZE) {
            LOG.info("[LeadBatch] Batch size threshold reached ({} leads). Triggering dispatch!", BATCH_SIZE);
            flushBatch(BATCH_SIZE);
        }
    }

    /**
     * Periodic fallback flush (every 30 minutes) so leads never get stuck
     * if volume is low and doesn't hit 10 right away.
     */
    @Scheduled(fixedDelay = 1800000, initialDelay = 60000)
    public synchronized void scheduledFlush() {
        if (!leadQueue.isEmpty()) {
            LOG.info("[LeadBatch] Scheduled flush executing for {} pending leads", leadQueue.size());
            flushBatch(leadQueue.size());
        }
    }

    /**
     * Drains up to maxItems from queue and dispatches batch notifications.
     */
    public synchronized void flushBatch(int maxItems) {
        List<LeadBatchItemDTO> batch = new ArrayList<>();
        while (!leadQueue.isEmpty() && batch.size() < maxItems) {
            LeadBatchItemDTO lead = leadQueue.poll();
            if (lead != null) {
                batch.add(lead);
            }
        }

        if (batch.isEmpty()) {
            return;
        }

        int count = batch.size();
        LOG.info("[LeadBatch] 🚀 Dispatching grouped batch notification for {} leads", count);

        String title = "🎯 " + count + " New Leads Received!";
        String namesPreview = batch.stream()
            .limit(3)
            .map(LeadBatchItemDTO::getName)
            .collect(Collectors.joining(", "));
        if (count > 3) {
            namesPreview += " and " + (count - 3) + " more";
        }
        String body = "Batch of " + count + " leads: " + namesPreview;

        // 1. Dispatch Email to all Admins
        if (crmEmailService != null && userRepository != null) {
            userRepository.findAll()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank()
                           && u.getAuthorities() != null
                           && u.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getName())))
                .flatMap(admin -> crmEmailService.sendBatchLeadAlertEmail(admin.getEmail(), batch))
                .subscribe(
                    v -> {},
                    err -> LOG.warn("[LeadBatchEmail] Failed to send batch email to admin: {}", err.getMessage())
                );
        }

        // 2. Dispatch grouped FCM Push Notification
        if (pushNotificationService != null) {
            Map<String, String> data = new HashMap<>();
            data.put("type", "NEW_LEAD_BATCH");
            data.put("batchCount", String.valueOf(count));
            data.put("route", "/leads");
            try {
                pushNotificationService.sendPushNotificationToAll(title, body, data);
            } catch (Exception e) {
                LOG.error("[LeadBatchPush] Push failed: {}", e.getMessage());
            }
        }

        // 3. Persist single grouped in-app CrmNotification for all users
        if (crmNotificationService != null && userRepository != null) {
            userRepository.findAll()
                .flatMap(user -> {
                    CrmNotification notif = new CrmNotification();
                    notif.setUserId(user.getLogin().toLowerCase());
                    notif.setTitle(title);
                    notif.setMessage(body);
                    notif.setType("NEW_LEAD");
                    notif.setReferenceId("batch-" + System.currentTimeMillis());
                    notif.setReferenceType("LEAD");
                    notif.setIsRead(Boolean.FALSE);
                    notif.setCreatedAt(Instant.now());
                    return crmNotificationService.createNotification(notif);
                })
                .subscribe(
                    saved -> LOG.debug("[LeadBatchNotification] Persisted batch notification for user: {}", saved.getUserId()),
                    err -> LOG.error("[LeadBatchNotification] Failed to persist batch notification: {}", err.getMessage())
                );
        }
    }

    public int getQueueSize() {
        return leadQueue.size();
    }
}
