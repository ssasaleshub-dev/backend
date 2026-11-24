package com.salescrm.service.impl;

import com.salescrm.service.CrmEmailService;
import com.salescrm.service.dto.EmailRequestDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MNC-standard reactive email service implementation.
 *
 * <p>All blocking JavaMail calls are offloaded to {@code Schedulers.boundedElastic()}
 * so the WebFlux event loop is never blocked.</p>
 *
 * <p>SMTP credentials are injected from environment variables at runtime:
 * <pre>
 *   MAIL_USERNAME=info@ssasaleshub.cloud
 *   MAIL_PASSWORD=your-app-password
 *   MAIL_FROM_NAME=SSA Sales Hub
 * </pre>
 * </p>
 */
@Service
public class CrmEmailServiceImpl implements CrmEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(CrmEmailServiceImpl.class);

    private static final String DATE_FORMAT = "dd MMM yyyy, hh:mm a";
    private static final String BRAND_COLOR = "#1a237e";
    private static final String BRAND_NAME  = "SSA SalesHub";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:ssasaleshub@gmail.com}")
    private String mailUsername = "ssasaleshub@gmail.com";

    @Value("${CRM_MAIL_FROM_NAME:" + BRAND_NAME + "}")
    private String mailFromName = BRAND_NAME;

    public CrmEmailServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ─── Core send ───────────────────────────────────────────────────────────

    @Override
    public Mono<Void> sendEmail(EmailRequestDTO request) {
        return Mono.fromRunnable(() -> doSend(
                request.getTo(),
                request.getCc(),
                request.getSubject(),
                request.getBody(),
                request.isHtml()
        )).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendBulkEmail(String toEmail, String subject, String htmlBody) {
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, htmlBody, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    // ─── Business-specific emails ─────────────────────────────────────────────

    @Override
    public Mono<Void> sendLeadAssignedEmail(String toEmail, String toName, String leadName, String assignedBy) {
        String subject = "New Lead Assigned: " + leadName;
        String body = buildHtml(
            "Lead Assigned to You",
            "Hi " + toName + ",",
            "A new lead has been assigned to you by <strong>" + assignedBy + "</strong>.",
            new String[][]{
                {"Lead Name",    leadName},
                {"Assigned By",  assignedBy},
                {"Assigned At",  LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))}
            },
            "Please log in to <a href='https://ssasaleshub.cloud' style='color:" + BRAND_COLOR + ";'>SSA Sales Hub</a> to view and follow up on this lead.",
            "View Lead", "https://ssasaleshub.cloud/leads"
        );
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendNewLeadAlertEmail(String toEmail, String leadName, String source, String phone) {
        String subject = "🔔 New Lead Received: " + leadName;
        String body = buildHtml(
            "New Lead Captured",
            "Hello,",
            "A new lead has just been captured in <strong>" + BRAND_NAME + "</strong>.",
            new String[][]{
                {"Lead Name", leadName},
                {"Source",    source},
                {"Phone",     phone != null ? phone : "N/A"},
                {"Received",  LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))}
            },
            "Log in to assign and manage this lead.",
            "View Leads", "https://ssasaleshub.cloud/leads"
        );
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendBatchLeadAlertEmail(String toEmail, java.util.List<com.salescrm.service.dto.LeadBatchItemDTO> leads) {
        if (leads == null || leads.isEmpty()) {
            return Mono.empty();
        }
        int count = leads.size();
        String subject = "\uD83C\uDFAF Batch of " + count + " New Leads Received \u2013 " + BRAND_NAME;

        StringBuilder tableRows = new StringBuilder();
        int idx = 1;
        for (com.salescrm.service.dto.LeadBatchItemDTO lead : leads) {
            tableRows.append("<tr style='border-bottom:1px solid #edf0f8;'>")
                .append("<td style='padding:10px 8px;text-align:center;color:#666;'>").append(idx++).append("</td>")
                .append("<td style='padding:10px 8px;font-weight:600;color:#222;'>").append(lead.getName()).append("</td>")
                .append("<td style='padding:10px 8px;color:#1a237e;'>").append(lead.getPhoneNumber()).append("</td>")
                .append("<td style='padding:10px 8px;color:#555;'><span style='background:#eef2ff;color:#3b82f6;padding:2px 8px;border-radius:12px;font-size:12px;'>").append(lead.getSource()).append("</span></td>")
                .append("<td style='padding:10px 8px;color:#888;font-size:12px;'>").append(lead.getCapturedAt()).append("</td>")
                .append("</tr>");
        }

        String tableHtml = "<table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;margin:16px 0;font-size:14px;\">" +
              "<thead><tr style=\"background:#f1f5f9;color:#475569;text-align:left;border-bottom:2px solid #cbd5e1;\">" +
              "<th style=\"padding:10px 8px;width:30px;text-align:center;\">#</th>" +
              "<th style=\"padding:10px 8px;\">Candidate Name</th>" +
              "<th style=\"padding:10px 8px;\">Phone</th>" +
              "<th style=\"padding:10px 8px;\">Source</th>" +
              "<th style=\"padding:10px 8px;\">Time</th>" +
              "</tr></thead><tbody>" +
              tableRows.toString() +
              "</tbody></table>";

        String body = buildBatchHtml(
            "Batch of " + count + " New Leads Captured",
            "Hello Administrator,",
            "A batch of <strong>" + count + " new leads</strong> has been captured and grouped together in <strong>" + BRAND_NAME + "</strong>:",
            tableHtml,
            "Log in to the CRM portal to assign these leads to sales counselors or start outreach.",
            "Review All Leads in CRM", "https://ssasaleshub.cloud/leads"
        );

        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendWelcomeEmail(String toEmail, String toName, String loginUsername, String tempPassword) {
        String subject = "Welcome to " + BRAND_NAME + " 🎉";
        String body = buildHtml(
            "Welcome Aboard!",
            "Hi " + toName + ",",
            "Your account has been created on <strong>" + BRAND_NAME + "</strong>. Here are your login details:",
            new String[][]{
                {"Username",         loginUsername},
                {"Temporary Password", tempPassword},
                {"Portal",           "https://ssasaleshub.cloud"}
            },
            "Please log in and change your password immediately for security.",
            "Login Now", "https://ssasaleshub.cloud"
        );
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendPasswordResetEmail(String toEmail, String toName, String resetLink) {
        String subject = BRAND_NAME + " – Password Reset Request";
        String body = buildHtml(
            "Password Reset",
            "Hi " + toName + ",",
            "We received a request to reset your <strong>" + BRAND_NAME + "</strong> password.",
            new String[][]{
                {"Valid For", "30 minutes"},
                {"Requested", LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))}
            },
            "If you did not request this, please ignore this email. Your password will remain unchanged.",
            "Reset Password", resetLink
        );
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    // ─── Internal SMTP dispatch ───────────────────────────────────────────────

    private void doSend(String to, String cc, String subject, String body, boolean html) {
        LOG.info("[EMAIL] Sending '{}' to '{}' | html={}", subject, to, html);
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name());

            helper.setFrom(new InternetAddress(mailUsername, mailFromName));
            helper.setTo(to);
            if (cc != null && !cc.isBlank()) {
                for (String addr : cc.split(",")) {
                    helper.addCc(addr.trim());
                }
            }
            helper.setSubject(subject);
            helper.setText(body, html);

            mailSender.send(msg);
            LOG.info("[EMAIL] ✅ Sent '{}' to '{}'", subject, to);

        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException e) {
            LOG.error("[EMAIL] ❌ Failed to send '{}' to '{}': {}", subject, to, e.getMessage(), e);
        }
    }

    // ─── HTML Template Builder ────────────────────────────────────────────────

    /**
     * Generates a branded HTML email body.
     *
     * @param heading      Bold header line inside the card
     * @param greeting     Greeting text (e.g. "Hi John,")
     * @param intro        Intro paragraph
     * @param details      2D array of [label, value] rows for the info table
     * @param footer       Footer note text
     * @param ctaLabel     Call-to-action button label
     * @param ctaUrl       Call-to-action URL
     */
    private String buildHtml(String heading, String greeting, String intro,
                              String[][] details, String footer, String ctaLabel, String ctaUrl) {
        StringBuilder rows = new StringBuilder();
        if (details != null) {
            for (String[] row : details) {
                rows.append("<tr>")
                    .append("<td style='padding:8px 12px;font-weight:600;color:#555;width:160px;'>").append(row[0]).append("</td>")
                    .append("<td style='padding:8px 12px;color:#222;'>").append(row[1]).append("</td>")
                    .append("</tr>");
            }
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
            <title>""" + heading + """
            </title></head>
            <body style="margin:0;padding:0;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" bgcolor="#f4f6fb">
                <tr><td align="center" style="padding:40px 16px;">
                  <table width="600" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 16px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <tr><td style="background:""" + BRAND_COLOR + """
                    ;padding:28px 36px;text-align:center;">
                      <h1 style="margin:0;color:#ffffff;font-size:22px;letter-spacing:1px;">""" + BRAND_NAME + """
                      </h1>
                    </td></tr>

                    <!-- Body -->
                    <tr><td style="padding:36px 36px 24px;">
                      <h2 style="margin:0 0 8px;color:""" + BRAND_COLOR + """
                      ;font-size:20px;">""" + heading + """
                      </h2>
                      <p style="color:#444;font-size:15px;line-height:1.6;margin:0 0 20px;">""" + greeting + """
                      </p>
                      <p style="color:#444;font-size:15px;line-height:1.6;margin:0 0 20px;">""" + intro + """
                      </p>

                      <!-- Details Table -->
                      <table width="100%" cellpadding="0" cellspacing="0" style="background:#f8f9ff;border-radius:8px;border:1px solid #e3e8ff;margin-bottom:24px;">
                        <tbody>""" + rows + """
                        </tbody>
                      </table>

                      <!-- CTA Button -->
                      <div style="text-align:center;margin:28px 0;">
                        <a href='""" + ctaUrl + """
                        ' style="display:inline-block;background:""" + BRAND_COLOR + """
                        ;color:#fff;padding:14px 36px;border-radius:6px;text-decoration:none;font-size:15px;font-weight:600;letter-spacing:0.5px;">""" + ctaLabel + """
                        </a>
                      </div>

                      <p style="color:#888;font-size:13px;line-height:1.6;margin:0;">""" + footer + """
                      </p>
                    </td></tr>

                    <!-- Footer -->
                    <tr><td style="background:#f4f6fb;padding:20px 36px;text-align:center;border-top:1px solid #e8ecf7;">
                      <p style="margin:0;color:#aaa;font-size:12px;">
                        &copy; """ + LocalDateTime.now().getYear() + " " + BRAND_NAME + """
                        &nbsp;|&nbsp; <a href='https://ssasaleshub.cloud' style='color:#aaa;'>ssasaleshub.cloud</a>
                      </p>
                      <p style="margin:4px 0 0;color:#ccc;font-size:11px;">This is an automated email. Please do not reply.</p>
                    </td></tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
    }

    private String buildBatchHtml(String heading, String greeting, String intro,
                                   String contentTableHtml, String footer, String ctaLabel, String ctaUrl) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1.0">
            <title>""" + heading + """
            </title></head>
            <body style="margin:0;padding:0;background:#f4f6fb;font-family:Arial,Helvetica,sans-serif;">
              <table width="100%" cellpadding="0" cellspacing="0" bgcolor="#f4f6fb">
                <tr><td align="center" style="padding:40px 16px;">
                  <table width="680" cellpadding="0" cellspacing="0" style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 16px rgba(0,0,0,0.08);">

                    <!-- Header -->
                    <tr><td style="background:""" + BRAND_COLOR + """
                    ;padding:28px 36px;text-align:center;">
                      <h1 style="margin:0;color:#ffffff;font-size:22px;letter-spacing:1px;">""" + BRAND_NAME + """
                      </h1>
                    </td></tr>

                    <!-- Body -->
                    <tr><td style="padding:36px 36px 24px;">
                      <h2 style="margin:0 0 8px;color:""" + BRAND_COLOR + """
                      ;font-size:20px;">""" + heading + """
                      </h2>
                      <p style="color:#444;font-size:15px;line-height:1.6;margin:0 0 10px;">""" + greeting + """
                      </p>
                      <p style="color:#444;font-size:15px;line-height:1.6;margin:0 0 16px;">""" + intro + """
                      </p>

                      <!-- Batch Table -->
                      """ + contentTableHtml + """

                      <!-- CTA Button -->
                      <div style="text-align:center;margin:28px 0;">
                        <a href='""" + ctaUrl + """
                        ' style="display:inline-block;background:""" + BRAND_COLOR + """
                        ;color:#fff;padding:14px 36px;border-radius:6px;text-decoration:none;font-size:15px;font-weight:600;letter-spacing:0.5px;">""" + ctaLabel + """
                        </a>
                      </div>

                      <p style="color:#888;font-size:13px;line-height:1.6;margin:0;">""" + footer + """
                      </p>
                    </td></tr>

                    <!-- Footer -->
                    <tr><td style="background:#f4f6fb;padding:20px 36px;text-align:center;border-top:1px solid #e8ecf7;">
                      <p style="margin:0;color:#aaa;font-size:12px;">
                        &copy; """ + LocalDateTime.now().getYear() + " " + BRAND_NAME + """
                        &nbsp;|&nbsp; <a href='https://ssasaleshub.cloud' style='color:#aaa;'>ssasaleshub.cloud</a>
                      </p>
                      <p style="margin:4px 0 0;color:#ccc;font-size:11px;">This is an automated batch summary email. Please do not reply.</p>
                    </td></tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """;
    }
}
