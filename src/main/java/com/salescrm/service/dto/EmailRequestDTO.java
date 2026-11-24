package com.salescrm.service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO for ad-hoc email send requests from admin/frontend.
 */
public class EmailRequestDTO {

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email address")
    private String to;

    @Size(max = 500, message = "CC list too long")
    private String cc;         // comma-separated, optional

    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject too long")
    private String subject;

    @NotBlank(message = "Body is required")
    private String body;

    private boolean isHtml = true;

    // ─── Getters & Setters ────────────────────────────────────────────────────

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getCc() { return cc; }
    public void setCc(String cc) { this.cc = cc; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isHtml() { return isHtml; }
    public void setHtml(boolean html) { isHtml = html; }

    @Override
    public String toString() {
        return "EmailRequestDTO{to='" + to + "', subject='" + subject + "'}";
    }
}
