package com.salescrm.service.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DTO representing an individual lead within a grouped notification batch.
 */
public class LeadBatchItemDTO implements Serializable {

    private String leadId;
    private String name;
    private String phoneNumber;
    private String source;
    private String capturedAt;

    public LeadBatchItemDTO() {
        this.capturedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a"));
    }

    public LeadBatchItemDTO(String leadId, String name, String phoneNumber, String source) {
        this.leadId = leadId;
        this.name = (name != null && !name.isBlank()) ? name : "Unknown Candidate";
        this.phoneNumber = (phoneNumber != null && !phoneNumber.isBlank()) ? phoneNumber : "N/A";
        this.source = (source != null && !source.isBlank()) ? source : "Direct";
        this.capturedAt = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM, hh:mm a"));
    }

    public String getLeadId() { return leadId; }
    public void setLeadId(String leadId) { this.leadId = leadId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getCapturedAt() { return capturedAt; }
    public void setCapturedAt(String capturedAt) { this.capturedAt = capturedAt; }

    @Override
    public String toString() {
        return "LeadBatchItemDTO{" +
            "leadId='" + leadId + '\'' +
            ", name='" + name + '\'' +
            ", phoneNumber='" + phoneNumber + '\'' +
            ", source='" + source + '\'' +
            '}';
    }
}
