package com.salescrm.service;

import com.salescrm.service.dto.EmailRequestDTO;
import reactor.core.publisher.Mono;

/**
 * MNC-standard Email Service interface.
 * All operations return Mono for WebFlux non-blocking compatibility.
 */
public interface CrmEmailService {

    /**
     * Send a plain-text or HTML email to any recipient.
     */
    Mono<Void> sendEmail(EmailRequestDTO request);

    /**
     * Send a lead-assigned notification email to a sales rep.
     */
    Mono<Void> sendLeadAssignedEmail(String toEmail, String toName, String leadName, String assignedBy);

    /**
     * Send a new-lead alert email to admin/team.
     */
    Mono<Void> sendNewLeadAlertEmail(String toEmail, String leadName, String source, String phone);

    /**
     * Send a new-lead alert email to admin/team with custom recipient name.
     */
    Mono<Void> sendNewLeadAlertEmail(String toEmail, String recipientName, String leadName, String source, String phone);

    /**
     * Send a consolidated batch lead-alert email containing up to 10 new leads to admin.
     */
    Mono<Void> sendBatchLeadAlertEmail(String toEmail, java.util.List<com.salescrm.service.dto.LeadBatchItemDTO> leads);

    /**
     * Send a consolidated batch lead-alert email containing up to 10 new leads to admin with custom recipient name.
     */
    Mono<Void> sendBatchLeadAlertEmail(String toEmail, String recipientName, java.util.List<com.salescrm.service.dto.LeadBatchItemDTO> leads);

    /**
     * Send a welcome email to a newly created user.
     */
    Mono<Void> sendWelcomeEmail(String toEmail, String toName, String loginUsername, String tempPassword);

    /**
     * Send a password-reset OTP email.
     */
    Mono<Void> sendPasswordResetEmail(String toEmail, String toName, String resetLink);

    /**
     * Send a new login session detection alert email.
     */
    Mono<Void> sendNewLoginAlertEmail(String toEmail, String userName, String device, String location, String ipAddress);

    /**
     * Send executive weekly performance report to an admin user.
     * Contains only aggregate counts, percentages, and inline trend graph.
     * STRICT ZERO DATA LEAKAGE: Never includes candidate personal info.
     */
    Mono<Void> sendWeeklyAdminReportEmail(String toEmail, String adminName, com.salescrm.service.dto.WeeklyReportStatsDTO stats);

    /**
     * Send calendar event or follow-up reminder notification email to a user.
     * Follows the official SSA Saleshub human UI design (clean typographic layout, no card-in-card boxes).
     *
     * @param toEmail recipient email address
     * @param userName recipient display name
     * @param eventTitle title of the scheduled event / reminder
     * @param eventDate formatted date string (e.g. "Nov 25, 2026")
     * @param eventTime formatted time string (e.g. "10:30 AM")
     * @param description event details or counselor notes
     * @param candidateName associated candidate / lead name (if applicable)
     * @param isScheduledConfirmation true if confirming a new reminder set, false if alert when due
     */
    Mono<Void> sendCalendarReminderEmail(
        String toEmail,
        String userName,
        String eventTitle,
        String eventDate,
        String eventTime,
        String description,
        String candidateName,
        boolean isScheduledConfirmation
    );

    default Mono<Void> sendCalendarReminderEmail(
        String toEmail,
        String userName,
        String eventTitle,
        String eventDate,
        String eventTime,
        String description,
        String candidateName,
        String meetingLink,
        boolean isScheduledConfirmation
    ) {
        return sendCalendarReminderEmail(toEmail, userName, eventTitle, eventDate, eventTime, description, candidateName, isScheduledConfirmation);
    }

    default Mono<Void> sendCalendarReminderEmail(
        String toEmail,
        String userName,
        String eventTitle,
        String eventDate,
        String eventTime,
        String description,
        String candidateName
    ) {
        return sendCalendarReminderEmail(toEmail, userName, eventTitle, eventDate, eventTime, description, candidateName, false);
    }

    /**
     * Send a staff invite email to install the SSA Saleshub Android app from Google Play Store.
     */
    Mono<Void> sendStaffAppInviteEmail(String toEmail, String staffName);

    /**
     * Send a custom bulk/marketing email (admin-triggered).
     */
    Mono<Void> sendBulkEmail(String toEmail, String subject, String htmlBody);
}
