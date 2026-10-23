package com.salescrm.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Domain entity for Events, Callbacks & Reminders in the CRM.
 */
@Table("crm_events_reminders")
public class CrmEvent implements Serializable, Persistable<UUID> {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private UUID id;

    @NotNull(message = "must not be null")
    @Column("user_id")
    private String userId;

    @NotNull(message = "must not be null")
    @Column("title")
    private String title;

    @Column("description")
    private String description;

    @NotNull(message = "must not be null")
    @Column("event_date")
    private LocalDate eventDate;

    @Column("event_time")
    private LocalTime eventTime = LocalTime.of(9, 0);

    @Column("event_type")
    private String eventType = "REMINDER";

    @Column("priority")
    private String priority = "MEDIUM";

    @Column("candidate_id")
    private Long candidateId;

    @Column("candidate_name")
    private String candidateName;

    @Column("is_completed")
    private Boolean isCompleted = Boolean.FALSE;

    @Column("notification_sent")
    private Boolean notificationSent = Boolean.FALSE;

    @Column("remind_before_minutes")
    private Integer remindBeforeMinutes = 15;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Column("updated_at")
    private Instant updatedAt = Instant.now();

    @Transient
    @JsonIgnore
    private boolean isPersisted = false;

    @Override
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isNew() {
        return !isPersisted;
    }

    public CrmEvent setIsPersisted() {
        this.isPersisted = true;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public LocalTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Long getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Long candidateId) {
        this.candidateId = candidateId;
    }

    public String getCandidateName() {
        return candidateName;
    }

    public void setCandidateName(String candidateName) {
        this.candidateName = candidateName;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public void setIsCompleted(Boolean completed) {
        isCompleted = completed;
    }

    public Boolean getNotificationSent() {
        return notificationSent;
    }

    public void setNotificationSent(Boolean notificationSent) {
        this.notificationSent = notificationSent;
    }

    public Integer getRemindBeforeMinutes() {
        return remindBeforeMinutes;
    }

    public void setRemindBeforeMinutes(Integer remindBeforeMinutes) {
        this.remindBeforeMinutes = remindBeforeMinutes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CrmEvent that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "CrmEvent{" +
            "id=" + id +
            ", userId='" + userId + '\'' +
            ", title='" + title + '\'' +
            ", eventDate=" + eventDate +
            ", eventTime=" + eventTime +
            ", eventType='" + eventType + '\'' +
            ", isCompleted=" + isCompleted +
            '}';
    }
}
