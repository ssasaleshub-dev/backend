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
     * Send a custom bulk/marketing email (admin-triggered).
     */
    Mono<Void> sendBulkEmail(String toEmail, String subject, String htmlBody);
}
