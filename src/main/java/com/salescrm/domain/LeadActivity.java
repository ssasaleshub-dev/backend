package com.salescrm.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain entity representing an audit log or activity record for a Lead.
 */
@Table("lead_activity")
public class LeadActivity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Column("lead_id")
    private Long leadId;

    @Size(max = 100)
    @Column("user_id")
    private String userId;

    @NotNull(message = "must not be null")
    @Size(max = 255)
    @Column("user_name")
    private String userName;

    @NotNull(message = "must not be null")
    @Size(max = 50)
    @Column("action_type")
    private String actionType; // STATUS_CHANGE, NOTE_ADDED, FIELD_UPDATED, OUTREACH, ASSIGNMENT

    @NotNull(message = "must not be null")
    @Size(max = 500)
    @Column("summary")
    private String summary;

    @Column("details")
    private String details;

    @Size(max = 100)
    @Column("field_name")
    private String fieldName;

    @Column("old_value")
    private String oldValue;

    @Column("new_value")
    private String newValue;

    @NotNull(message = "must not be null")
    @Column("created_at")
    private Instant createdAt = Instant.now();

    public Long getId() {
        return this.id;
    }

    public LeadActivity id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLeadId() {
        return this.leadId;
    }

    public LeadActivity leadId(Long leadId) {
        this.setLeadId(leadId);
        return this;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public String getUserId() {
        return this.userId;
    }

    public LeadActivity userId(String userId) {
        this.setUserId(userId);
        return this;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return this.userName;
    }

    public LeadActivity userName(String userName) {
        this.setUserName(userName);
        return this;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getActionType() {
        return this.actionType;
    }

    public LeadActivity actionType(String actionType) {
        this.setActionType(actionType);
        return this;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getSummary() {
        return this.summary;
    }

    public LeadActivity summary(String summary) {
        this.setSummary(summary);
        return this;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDetails() {
        return this.details;
    }

    public LeadActivity details(String details) {
        this.setDetails(details);
        return this;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getFieldName() {
        return this.fieldName;
    }

    public LeadActivity fieldName(String fieldName) {
        this.setFieldName(fieldName);
        return this;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return this.oldValue;
    }

    public LeadActivity oldValue(String oldValue) {
        this.setOldValue(oldValue);
        return this;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return this.newValue;
    }

    public LeadActivity newValue(String newValue) {
        this.setNewValue(newValue);
        return this;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public LeadActivity createdAt(Instant createdAt) {
        this.setCreatedAt(createdAt);
        return this;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LeadActivity)) {
            return false;
        }
        return id != null && id.equals(((LeadActivity) o).id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "LeadActivity{" +
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
