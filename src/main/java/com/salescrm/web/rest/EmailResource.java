package com.salescrm.web.rest;

import com.salescrm.service.CrmEmailService;
import com.salescrm.service.dto.EmailRequestDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * REST controller for ad-hoc email operations.
 * Requires ADMIN or STAFF role for security.
 *
 * Base path: /api/email
 */
@RestController
@RequestMapping("/api/email")
public class EmailResource {

    private static final Logger LOG = LoggerFactory.getLogger(EmailResource.class);

    private final CrmEmailService emailService;

    public EmailResource(CrmEmailService emailService) {
        this.emailService = emailService;
    }

    /**
     * POST /api/email/send
     * Send a custom email to any recipient (admin/staff only).
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
    public Mono<ResponseEntity<Map<String, String>>> sendEmail(
            @Valid @RequestBody EmailRequestDTO request) {

        LOG.info("[EmailResource] Send email request to: {}", request.getTo());

        return emailService.sendEmail(request)
                .thenReturn(ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Email sent to " + request.getTo()
                )))
                .onErrorResume(ex -> {
                    LOG.error("[EmailResource] Failed to send email", ex);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(Map.of("status", "error", "message", ex.getMessage())));
                });
    }

    /**
     * POST /api/email/lead-alert
     * Trigger a new-lead alert email (called internally or via webhook).
     */
    @PostMapping("/lead-alert")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> sendLeadAlert(
            @RequestBody Map<String, String> body) {

        String toEmail   = body.getOrDefault("toEmail", "");
        String leadName  = body.getOrDefault("leadName", "Unknown");
        String source    = body.getOrDefault("source", "Direct");
        String phone     = body.getOrDefault("phone", "");

        return emailService.sendNewLeadAlertEmail(toEmail, leadName, source, phone)
                .thenReturn(ResponseEntity.ok(Map.of("status", "success")));
    }

    /**
     * POST /api/email/welcome
     * Send a welcome email to a new user (admin only).
     */
    @PostMapping("/welcome")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> sendWelcome(
            @RequestBody Map<String, String> body) {

        String toEmail      = body.getOrDefault("toEmail", "");
        String toName       = body.getOrDefault("toName", "User");
        String loginUsername = body.getOrDefault("loginUsername", "");
        String tempPassword = body.getOrDefault("tempPassword", "");

        return emailService.sendWelcomeEmail(toEmail, toName, loginUsername, tempPassword)
                .thenReturn(ResponseEntity.ok(Map.of("status", "success", "message", "Welcome email sent")));
    }

    /**
     * POST /api/email/test
     * Send a test email to verify SMTP config (admin only).
     */
    @PostMapping("/test")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> testEmail(
            @RequestBody Map<String, String> body) {

        String toEmail = body.getOrDefault("toEmail", "");
        LOG.info("[EmailResource] Test email to: {}", toEmail);

        EmailRequestDTO req = new EmailRequestDTO();
        req.setTo(toEmail);
        req.setSubject("✅ SSA Sales Hub – SMTP Test Email");
        req.setBody("<h2>SMTP is working!</h2><p>Your email configuration is correctly set up for <strong>SSA Sales Hub</strong>.</p>");
        req.setHtml(true);

        return emailService.sendEmail(req)
                .thenReturn(ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Test email sent to " + toEmail
                )));
    }

    /**
     * POST /api/email/test-batch
     * Send a sample 10-lead batch alert email to verify the grouped format (admin only).
     */
    @PostMapping("/test-batch")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Mono<ResponseEntity<Map<String, String>>> testBatchEmail(
            @RequestBody Map<String, String> body) {

        String toEmail = body.getOrDefault("toEmail", "");
        LOG.info("[EmailResource] Test 10-lead batch email to: {}", toEmail);

        java.util.List<com.salescrm.service.dto.LeadBatchItemDTO> sampleBatch = new java.util.ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            sampleBatch.add(new com.salescrm.service.dto.LeadBatchItemDTO(
                String.valueOf(1000 + i),
                "Candidate " + i + " (Sample)",
                "+91 98765 4321" + (i % 10),
                (i % 2 == 0) ? "Meta Instagram" : "Meta Facebook"
            ));
        }

        return emailService.sendBatchLeadAlertEmail(toEmail, sampleBatch)
                .thenReturn(ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "Sample 10-lead batch email dispatched to " + toEmail
                )));
    }
}
