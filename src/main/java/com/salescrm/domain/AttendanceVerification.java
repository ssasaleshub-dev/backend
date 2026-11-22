package com.salescrm.domain;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A one-time attendance challenge. The staff member's registered phone must sign it with its
 * fingerprint-locked key before it expires; only then is the check-in recorded.
 */
@Table("attendance_verification")
public class AttendanceVerification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    public static final String ACTION_CHECK_IN = "CHECK_IN";

    @Id
    @Column("id")
    private Long id;

    @Column("user_id")
    private String userId;

    @Column("action")
    private String action;

    @Column("nonce")
    private String nonce;

    @Column("status")
    private String status;

    /** WEB (approve on phone) or APP (verified directly on the phone). */
    @Column("source")
    private String source;

    @Column("work_mode")
    private String workMode;

    @Column("note")
    private String note;

    /** Browser / device description of where the request was started. */
    @Column("requested_from")
    private String requestedFrom;

    @Column("approved_device_id")
    private String approvedDeviceId;

    @Column("created_at")
    private Instant createdAt;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("resolved_at")
    private Instant resolvedAt;

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getRequestedFrom() {
        return requestedFrom;
    }

    public void setRequestedFrom(String requestedFrom) {
        this.requestedFrom = requestedFrom;
    }

    public String getApprovedDeviceId() {
        return approvedDeviceId;
    }

    public void setApprovedDeviceId(String approvedDeviceId) {
        this.approvedDeviceId = approvedDeviceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
