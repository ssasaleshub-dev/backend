package com.salescrm.service.impl;

import com.salescrm.service.CrmEmailService;
import com.salescrm.service.dto.EmailRequestDTO;
import com.salescrm.service.dto.LeadBatchItemDTO;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * MNC-standard reactive email service implementation.
 *
 * <p><strong>Data Privacy & Security Standard:</strong>
 * Candidate PII (names, phone numbers, contact info) is NEVER transmitted in unencrypted emails.
 * Emails act strictly as secure arrival alerts prompting authorized staff to log in to the CRM.
 * </p>
 *
 * <p>Complies with Google & RFC 5322 anti-spam delivery standards:
 * <ul>
 *   <li>Multipart/alternative (plain-text fallback + HTML) to avoid spam filters.</li>
 *   <li>Clean non-spammy subject headers without emojis.</li>
 *   <li>Standard transactional headers (Auto-Submitted, List-Unsubscribe, Precedence).</li>
 *   <li>Inline brand logo CID embedding for maximum client compatibility.</li>
 *   <li>All blocking JavaMail calls offloaded to {@code Schedulers.boundedElastic()}.</li>
 * </ul>
 * </p>
 */
@Service
public class CrmEmailServiceImpl implements CrmEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(CrmEmailServiceImpl.class);

    private static final String DATE_FORMAT = "dd MMM yyyy, hh:mm a";
    private static final String BRAND_NAME  = "SSA Saleshub";

    // Logo Emerald & Gold Theme Constants
    private static final String LOGO_GRADIENT_HEADER =
        "background-color:#047857;background:linear-gradient(135deg,#064e3b 0%,#047857 55%,#059669 100%);border-bottom:3px solid #facc15;";
    private static final String LOGO_BUTTON_GRADIENT =
        "background-color:#059669;background:linear-gradient(135deg,#10b981 0%,#047857 100%);color:#ffffff;box-shadow:0 4px 14px rgba(5,150,105,0.35);border:1px solid #34d399;";

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
                null,
                request.getBody(),
                request.isHtml()
        )).subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendBulkEmail(String toEmail, String subject, String htmlBody) {
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, null, htmlBody, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    // ─── Business-specific emails (ZERO DATA LEAKAGE STANDARD) ────────────────

    @Override
    public Mono<Void> sendLeadAssignedEmail(String toEmail, String toName, String leadName, String assignedBy) {
        String subject = "[SSA Saleshub] Lead Assigned to Your Queue";
        String body = buildHtml(
            "New Lead Assigned",
            "Hi " + toName + ",",
            "A new lead inquiry has been assigned to you by <strong>" + assignedBy + "</strong>.",
            new String[][]{
                {"Assigned By",  assignedBy},
                {"Assigned At",  LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))},
                {"Data Privacy", "Candidate contact details protected in CRM"}
            },
            null,
            "View Lead in CRM", "https://ssasaleshub.cloud/leads"
        );
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, null, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    private String resolveGreeting(String recipientName) {
        if (recipientName == null || recipientName.isBlank()) {
            return "Hello Administrator,";
        }
        String clean = recipientName.trim();
        if (clean.equalsIgnoreCase("admin") || clean.equalsIgnoreCase("administrator") || clean.equalsIgnoreCase("role_admin")) {
            return "Hello Administrator,";
        }
        if (clean.length() == 1) {
            clean = clean.toUpperCase();
        } else if (Character.isLowerCase(clean.charAt(0))) {
            clean = Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
        }
        return "Hello " + clean + ",";
    }

    @Override
    public Mono<Void> sendNewLeadAlertEmail(String toEmail, String leadName, String source, String phone) {
        return sendNewLeadAlertEmail(toEmail, null, leadName, source, phone);
    }

    @Override
    public Mono<Void> sendNewLeadAlertEmail(String toEmail, String recipientName, String leadName, String source, String phone) {
        String greeting = resolveGreeting(recipientName);
        String subject = "[SSA Saleshub] New Lead Inquiry Arrived";
        String body = buildHtml(
            "New Lead Inquiry Arrived",
            greeting,
            "A new candidate inquiry has arrived in <strong>" + BRAND_NAME + "</strong>.",
            new String[][]{
                {"Source Channel", (source != null && !source.isBlank()) ? source : "Direct"},
                {"Received At",    LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))},
                {"Data Privacy",   "Candidate contact details protected in CRM"}
            },
            null,
            "Open CRM to View Lead", "https://ssasaleshub.cloud/leads"
        );
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, null, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendBatchLeadAlertEmail(String toEmail, List<LeadBatchItemDTO> leads) {
        return sendBatchLeadAlertEmail(toEmail, null, leads);
    }

    @Override
    public Mono<Void> sendBatchLeadAlertEmail(String toEmail, String recipientName, List<LeadBatchItemDTO> leads) {
        if (leads == null || leads.isEmpty()) {
            return Mono.empty();
        }
        int count = leads.size();
        String subject = "[SSA Saleshub] " + count + " New Leads Received";

        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        String greeting = resolveGreeting(recipientName);

        // Simple, human plain text fallback
        String plainText = "SSA Saleshub\n\n" +
            greeting + "\n\n" +
            "You have " + count + " new leads waiting in your CRM account.\n\n" +
            "Received: " + currentTime + "\n\n" +
            "View leads: https://ssasaleshub.cloud/leads\n";

        // Simple, clean human UI summary table (responsive for mobile & desktop)
        String summaryTableHtml = """
            <table cellpadding="0" cellspacing="0" border="0" width="100%" class="summary-tbl" style="background:#f8fafc;border-radius:10px;border:1px solid #e2e8f0;margin:18px 0;width:100%;">
              <tr>
                <td style="padding:12px 18px;color:#64748b;font-size:13px;white-space:nowrap;border-bottom:1px solid #f1f5f9;width:35%;">New Leads</td>
                <td align="right" style="padding:12px 18px;color:#0f172a;font-size:15px;font-weight:700;white-space:nowrap;border-bottom:1px solid #f1f5f9;text-align:right;">""" + count + """
                </td>
              </tr>
              <tr>
                <td style="padding:12px 18px;color:#64748b;font-size:13px;white-space:nowrap;width:35%;">Received At</td>
                <td align="right" style="padding:12px 18px;color:#334155;font-size:13px;font-weight:500;white-space:nowrap;text-align:right;">""" + currentTime + """
                </td>
              </tr>
            </table>
            """;

        String body = buildBatchHtml(
            count + " New Leads Received",
            greeting,
            "You have <strong>" + count + " new leads</strong> waiting in your CRM account. Log in to review and assign them to sales counselors:",
            summaryTableHtml,
            null,
            "View Leads in CRM \u2192", "https://ssasaleshub.cloud/leads"
        );

        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, plainText, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendWelcomeEmail(String toEmail, String toName, String loginUsername, String tempPassword) {
        String subject = "[SSA Saleshub] Account Created: Login Credentials";
        String body = buildHtml(
            "Welcome to SSA Saleshub",
            "Hi " + toName + ",",
            "Your user account has been successfully created. Here are your login details:",
            new String[][]{
                {"Username",           loginUsername},
                {"Temporary Password", tempPassword},
                {"Portal URL",         "https://ssasaleshub.cloud"}
            },
            null,
            "Log In to CRM", "https://ssasaleshub.cloud"
        );
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, null, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendPasswordResetEmail(String toEmail, String toName, String resetLink) {
        String subject = "[SSA Saleshub] Password Reset Request";
        String body = buildHtml(
            "Password Reset Request",
            "Hi " + toName + ",",
            "We received a request to reset your password for <strong>" + BRAND_NAME + "</strong>.",
            new String[][]{
                {"Link Validity", "30 minutes"},
                {"Requested At",  LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))}
            },
            null,
            "Reset Password", resetLink
        );
        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, null, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    @Override
    public Mono<Void> sendNewLoginAlertEmail(String toEmail, String userName, String device, String location, String ipAddress) {
        String greeting = resolveGreeting(userName);
        String subject = "SSA Saleshub Security Alert: New Sign-in Detected";
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT));

        String dev = (device != null && !device.isBlank()) ? device : "Web Browser";
        String loc = (location != null && !location.isBlank()) ? location : "Unknown Location";
        if (ipAddress != null && !ipAddress.isBlank()) {
            loc += " (" + ipAddress + ")";
        }

        String[][] details = new String[][]{
            {"Device",   dev},
            {"Time",     currentTime},
            {"Location", loc}
        };

        String footer = "If this was you, no action is needed. If you did not sign in, please secure your account immediately.";

        String body = buildHtml(
            "New Sign-in Detected",
            greeting,
            "A new login session was detected on your account. Was this you?",
            details,
            footer,
            "Secure My Account \u2192", "https://ssasaleshub.cloud/reset/request"
        );

        String plainText = "SSA Saleshub - Security Alert\n\n" +
            greeting + "\n\n" +
            "A new login session was detected on your account. Was this you?\n\n" +
            "Device: " + dev + "\n" +
            "Time: " + currentTime + "\n" +
            "Location: " + loc + "\n\n" +
            "If this was you, you can safely ignore this email.\n" +
            "If you did not sign in, secure your account: https://ssasaleshub.cloud/reset/request\n";

        return Mono.fromRunnable(() -> doSend(toEmail, null, subject, plainText, body, true))
                   .subscribeOn(Schedulers.boundedElastic()).then();
    }

    // ─── Internal SMTP dispatch with Anti-Spam Headers ─────────────────────────

    private void doSend(String to, String cc, String subject, String plainText, String htmlBody, boolean html) {
        LOG.info("[EMAIL] Sending '{}' to '{}' | html={}", subject, to, html);
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                msg,
                MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                StandardCharsets.UTF_8.name()
            );

            helper.setFrom(new InternetAddress(mailUsername, mailFromName, StandardCharsets.UTF_8.name()));
            helper.setTo(to);
            if (cc != null && !cc.isBlank()) {
                for (String addr : cc.split(",")) {
                    helper.addCc(addr.trim());
                }
            }
            helper.setSubject(subject);

            // Google Anti-Spam Standard: Provide both clean plain-text and HTML
            if (html) {
                String cleanPlain = (plainText != null && !plainText.isBlank())
                    ? plainText
                    : htmlBody.replaceAll("<[^>]*>", " ").replaceAll("\\s+", " ").trim();
                helper.setText(cleanPlain, htmlBody);
            } else {
                helper.setText(htmlBody, false);
            }

            // Google & RFC 5322 Anti-Spam Headers
            msg.setHeader("Auto-Submitted", "auto-generated");
            msg.setHeader("X-Auto-Response-Suppress", "All");
            msg.setHeader("Precedence", "bulk");
            msg.setHeader("List-Unsubscribe", "<mailto:" + mailUsername + "?subject=unsubscribe>");
            msg.setHeader("X-Report-Abuse-To", mailUsername);
            msg.setHeader("X-Entity-Ref-ID", UUID.randomUUID().toString());

            // Brand Logo CID Embedding (with fallback to web URL)
            try {
                ClassPathResource logoResource = new ClassPathResource("static/brand-logo.jpg");
                if (logoResource.exists()) {
                    helper.addInline("brandLogo", logoResource, "image/jpeg");
                }
            } catch (Exception logoEx) {
                LOG.debug("[EMAIL] Logo inline attach skipped: {}", logoEx.getMessage());
            }

            mailSender.send(msg);
            LOG.info("[EMAIL] ✅ Sent '{}' to '{}'", subject, to);

        } catch (MailException | MessagingException | java.io.UnsupportedEncodingException e) {
            LOG.error("[EMAIL] ❌ Failed to send '{}' to '{}': {}", subject, to, e.getMessage(), e);
        }
    }

    // ─── Modern Logo Emerald Gradient Template Builders ───────────────────────

    private String buildHtml(String heading, String greeting, String intro,
                              String[][] details, String footer, String ctaLabel, String ctaUrl) {
        String footerHtml = (footer != null && !footer.isBlank())
            ? "<p style=\"color:#64748b;font-size:13px;line-height:1.6;margin:0;padding-top:16px;border-top:1px solid #f1f5f9;\">" + footer + "</p>"
            : "";
        StringBuilder rows = new StringBuilder();
        if (details != null) {
            for (String[] row : details) {
                rows.append("<tr>")
                    .append("<td style='padding:10px 14px;font-weight:600;color:#166534;width:150px;border-bottom:1px solid #e2e8f0;font-size:13px;'>").append(row[0]).append("</td>")
                    .append("<td style='padding:10px 14px;color:#0f172a;border-bottom:1px solid #e2e8f0;font-size:14px;font-weight:500;'>").append(row[1]).append("</td>")
                    .append("</tr>");
            }
        }

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1.0">
              <meta name="format-detection" content="telephone=no">
              <title>""" + heading + """
              </title>
              <style>
                @media only screen and (max-width: 600px) {
                  .email-wrapper { padding: 12px 6px !important; }
                  .email-card { width: 100% !important; max-width: 100% !important; border-radius: 12px !important; }
                  .header-box { padding: 14px 16px !important; }
                  .brand-title { font-size: 16px !important; }
                  .brand-sub { font-size: 10px !important; }
                  .body-box { padding: 20px 16px !important; }
                  .summary-tbl td { padding: 10px 12px !important; font-size: 13px !important; }
                  .cta-btn { display: block !important; width: 100% !important; box-sizing: border-box !important; text-align: center !important; padding: 12px 18px !important; }
                  .footer-box { padding: 16px 14px !important; }
                }
              </style>
            </head>
            <body style="margin:0;padding:0;background-color:#f8fafc;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0f172a;-webkit-font-smoothing:antialiased;">
              <table width="100%" cellpadding="0" cellspacing="0" border="0" bgcolor="#f8fafc" class="email-wrapper" style="padding:24px 10px;margin:0 auto;width:100% !important;">
                <tr>
                  <td align="center">
                    <table width="100%" cellpadding="0" cellspacing="0" border="0" class="email-card" style="max-width:580px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 4px 20px rgba(0,0,0,0.04);">

                      <!-- Brand Header with Logo Emerald Gradient -->
                      <tr>
                        <td class="header-box" style=\"""" + LOGO_GRADIENT_HEADER + """
                        padding:16px 20px;">
                          <table cellpadding="0" cellspacing="0" border="0" width="100%">
                            <tr>
                              <td style="vertical-align:middle;width:40px;min-width:40px;">
                                <img src="cid:brandLogo" width="38" height="38" alt="SSA Logo" style="display:block;border-radius:10px;border:1.5px solid rgba(250,204,21,0.6);box-shadow:0 2px 8px rgba(0,0,0,0.25);" />
                              </td>
                              <td style="vertical-align:middle;padding-left:12px;">
                                <div class="brand-title" style="color:#ffffff;font-size:17px;font-weight:800;letter-spacing:-0.2px;line-height:1.2;white-space:nowrap;">""" + BRAND_NAME + """
                                </div>
                                <div class="brand-sub" style="color:#fef08a;font-size:11px;font-weight:500;margin-top:2px;white-space:nowrap;">Study &bull; Work &bull; Travel</div>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                      <!-- Body Content -->
                      <tr>
                        <td class="body-box" style="padding:24px 24px 20px;">
                          <h2 style="margin:0 0 8px;color:#065f46;font-size:20px;font-weight:700;letter-spacing:-0.3px;">""" + heading + """
                          </h2>
                          <p style="color:#475569;font-size:14px;line-height:1.6;margin:0 0 10px;">""" + greeting + """
                          </p>
                          <p style="color:#475569;font-size:14px;line-height:1.6;margin:0 0 20px;">""" + intro + """
                          </p>

                          <!-- Details Table -->
                          <table width="100%" cellpadding="0" cellspacing="0" style="background:#f0fdf4;border-radius:10px;border:1px solid #dcfce7;margin-bottom:24px;">
                            <tbody>""" + rows + """
                            </tbody>
                          </table>

                          <!-- CTA Button with Logo Emerald Gradient -->
                          <div style="text-align:center;margin:28px 0 20px;">
                            <a href='""" + ctaUrl + """
                            ' class="cta-btn" style="display:inline-block;""" + LOGO_BUTTON_GRADIENT + """
                            padding:12px 30px;border-radius:8px;text-decoration:none;font-size:14px;font-weight:700;letter-spacing:0.3px;">""" + ctaLabel + """
                            </a>
                          </div>

                          """ + footerHtml + """
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td class="footer-box" style="background:#f8fafc;padding:18px 24px;text-align:center;border-top:1px solid #e2e8f0;">
                          <p style="margin:0;color:#64748b;font-size:12px;line-height:1.6;">
                            &copy; """ + LocalDateTime.now().getYear() + " " + BRAND_NAME + """
                            &nbsp;&bull;&nbsp; <a href='https://ssasaleshub.cloud' style='color:#059669;font-weight:600;text-decoration:none;'>ssasaleshub.cloud</a>
                          </p>
                          <p style="margin:4px 0 0;color:#94a3b8;font-size:11px;">This is an automated operational notification. Please do not reply.</p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
    }

    private String buildBatchHtml(String heading, String greeting, String intro,
                                   String contentBlockHtml, String footer, String ctaLabel, String ctaUrl) {
        String footerHtml = (footer != null && !footer.isBlank())
            ? "<p style=\"color:#64748b;font-size:13px;line-height:1.6;margin:0;padding-top:16px;border-top:1px solid #f1f5f9;\">" + footer + "</p>"
            : "";

        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width,initial-scale=1.0">
              <meta name="format-detection" content="telephone=no">
              <title>""" + heading + """
              </title>
              <style>
                @media only screen and (max-width: 600px) {
                  .email-wrapper { padding: 12px 6px !important; }
                  .email-card { width: 100% !important; max-width: 100% !important; border-radius: 12px !important; }
                  .header-box { padding: 14px 16px !important; }
                  .brand-title { font-size: 16px !important; }
                  .brand-sub { font-size: 10px !important; }
                  .body-box { padding: 20px 16px !important; }
                  .summary-tbl td { padding: 10px 12px !important; font-size: 13px !important; }
                  .cta-btn { display: block !important; width: 100% !important; box-sizing: border-box !important; text-align: center !important; padding: 12px 18px !important; }
                  .footer-box { padding: 16px 14px !important; }
                }
              </style>
            </head>
            <body style="margin:0;padding:0;background-color:#f8fafc;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#0f172a;-webkit-font-smoothing:antialiased;">
              <table width="100%" cellpadding="0" cellspacing="0" border="0" bgcolor="#f8fafc" class="email-wrapper" style="padding:24px 10px;margin:0 auto;width:100% !important;">
                <tr>
                  <td align="center">
                    <table width="100%" cellpadding="0" cellspacing="0" border="0" class="email-card" style="max-width:580px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #e2e8f0;box-shadow:0 4px 20px rgba(0,0,0,0.04);">

                      <!-- Brand Header with Logo Emerald Gradient & Warm Gold Stripe -->
                      <tr>
                        <td class="header-box" style=\"""" + LOGO_GRADIENT_HEADER + """
                        padding:16px 20px;">
                          <table cellpadding="0" cellspacing="0" border="0" width="100%">
                            <tr>
                              <td style="vertical-align:middle;width:40px;min-width:40px;">
                                <img src="cid:brandLogo" width="38" height="38" alt="SSA Logo" style="display:block;border-radius:10px;border:1.5px solid rgba(250,204,21,0.6);box-shadow:0 2px 8px rgba(0,0,0,0.25);" />
                              </td>
                              <td style="vertical-align:middle;padding-left:12px;">
                                <div class="brand-title" style="color:#ffffff;font-size:17px;font-weight:800;letter-spacing:-0.2px;line-height:1.2;white-space:nowrap;">""" + BRAND_NAME + """
                                </div>
                                <div class="brand-sub" style="color:#fef08a;font-size:11px;font-weight:500;margin-top:2px;white-space:nowrap;">Study &bull; Work &bull; Travel</div>
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>

                      <!-- Body Content -->
                      <tr>
                        <td class="body-box" style="padding:24px 24px 20px;">
                          <h2 style="margin:0 0 8px;color:#065f46;font-size:20px;font-weight:700;letter-spacing:-0.3px;">""" + heading + """
                          </h2>
                          <p style="color:#475569;font-size:14px;line-height:1.6;margin:0 0 10px;">""" + greeting + """
                          </p>
                          <p style="color:#475569;font-size:14px;line-height:1.6;margin:0 0 20px;">""" + intro + """
                          </p>

                          <!-- Content Block -->
                          """ + contentBlockHtml + """

                          <!-- Call To Action Button with Emerald Gradient -->
                          <div style="text-align:center;margin:28px 0 20px;">
                            <a href='""" + ctaUrl + """
                            ' class="cta-btn" style="display:inline-block;""" + LOGO_BUTTON_GRADIENT + """
                            padding:12px 30px;border-radius:8px;text-decoration:none;font-size:14px;font-weight:700;letter-spacing:0.3px;">""" + ctaLabel + """
                            </a>
                          </div>

                          """ + footerHtml + """
                        </td>
                      </tr>

                      <!-- Footer -->
                      <tr>
                        <td class="footer-box" style="background:#f8fafc;padding:18px 24px;text-align:center;border-top:1px solid #e2e8f0;">
                          <p style="margin:0;color:#64748b;font-size:12px;line-height:1.6;">
                            &copy; """ + LocalDateTime.now().getYear() + " " + BRAND_NAME + """
                            &nbsp;&bull;&nbsp; <a href='https://ssasaleshub.cloud' style='color:#059669;font-weight:600;text-decoration:none;'>ssasaleshub.cloud</a>
                          </p>
                          <p style="margin:4px 0 0;color:#94a3b8;font-size:11px;">This is an automated operational notification. To update your alerts, log in to your dashboard.</p>
                        </td>
                      </tr>

                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """;
    }
}
