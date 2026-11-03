package com.salescrm.service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * A DTO representing a LeadActivity record.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LeadActivityDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private Long leadId;

    @Size(max = 100)
    private String userId;

    @Size(max = 255)
    private String userName;

    @Size(max = 50)
    private String actionType; // STATUS_CHANGE, NOTE_ADDED, FIELD_UPDATED, OUTREACH, ASSIGNMENT

    @Size(max = 500)
    private String summary;

    private String details;

    @Size(max = 100)
    private String fieldName;

    private String oldValue;

    private String newValue;

    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @JsonProperty("id")
    public void setIdFromJson(Object rawId) {
        if (rawId instanceof Number n) {
            this.id = n.longValue();
        } else if (rawId instanceof String s) {
            try {
                this.id = Long.parseLong(s.trim());
            } catch (NumberFormatException ignored) {
                // Client-generated string ids (e.g. act_17937...) are ignored so database auto-assigns primary key
                this.id = null;
            }
        } else {
            this.id = null;
        }
    }

    public Long getLeadId() {
        return leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getSummary() {
        return summary;
    }

    @JsonAlias("title")
    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetails() {
        return details;
    }

    @JsonAlias("description")
    public void setDetails(String details) {
        this.details = details;
    }

    public String getFieldName() {
        return fieldName;
    }

    @JsonAlias("fieldChanged")
    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @JsonAlias("timestamp")
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LeadActivityDTO)) {
            return false;
        }
        return id != null && id.equals(((LeadActivityDTO) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "LeadActivityDTO{" +
            "id=" + getId() +
            ", leadId=" + getLeadId() +
            ", userId='" + getUserId() + "'" +
            ", userName='" + getUserName() + "'" +
            ", actionType='" + getActionType() + "'" +
            ", summary='" + getSummary() + "'" +
            ", details='" + getDetails() + "'" +
            ", fieldName='" + getFieldName() + "'" +
            ", oldValue='" + getOldValue() + "'" +
            ", newValue='" + getNewValue() + "'" +
            ", createdAt='" + getCreatedAt() + "'" +
            "}";
    }
}
