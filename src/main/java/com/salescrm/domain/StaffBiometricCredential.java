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
 * Enterprise biometric credential registration for staff attendance.
 * Binds a staff member's enrolled fingerprint (Keystore Key ID) to an approved office Android device.
 * Locked by Admin to prevent proxy punching or unauthorized remote check-in.
 */
@Table("staff_biometric_credentials")
public class StaffBiometricCredential implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Size(max = 100)
    @Column("user_id")
    private String userId;

    @Size(max = 255)
    @Column("user_name")
    private String userName;

    @NotNull(message = "must not be null")
    @Size(max = 150)
    @Column("device_id")
    private String deviceId;

    @Size(max = 150)
    @Column("device_name")
    private String deviceName;

    @NotNull(message = "must not be null")
    @Size(max = 500)
    @Column("biometric_key_id")
    private String biometricKeyId;

    @NotNull(message = "must not be null")
    @Size(max = 50)
    @Column("status")
    private String status = "PENDING_APPROVAL"; // PENDING_APPROVAL, LOCKED, REVOKED

    @Column("is_locked")
    private Boolean isLocked = Boolean.FALSE;

    @Column("locked_at")
    private Instant lockedAt;

    @Size(max = 100)
    @Column("locked_by")
    private String lockedBy;

    @Column("created_at")
    private Instant createdAt = Instant.now();

    @Column("updated_at")
    private Instant updatedAt = Instant.now();

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

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getBiometricKeyId() {
        return biometricKeyId;
    }

    public void setBiometricKeyId(String biometricKeyId) {
        this.biometricKeyId = biometricKeyId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getIsLocked() {
        return isLocked;
    }

    public void setIsLocked(Boolean locked) {
        isLocked = locked;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(Instant lockedAt) {
        this.lockedAt = lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(String lockedBy) {
        this.lockedBy = lockedBy;
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
        if (!(o instanceof StaffBiometricCredential that)) return false;
        return Objects.equals(id, that.id) ||
            (Objects.equals(userId, that.userId) && Objects.equals(deviceId, that.deviceId));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, userId, deviceId);
    }

    @Override
    public String toString() {
        return "StaffBiometricCredential{" +
            "id=" + id +
            ", userId='" + userId + '\'' +
            ", deviceId='" + deviceId + '\'' +
            ", status='" + status + '\'' +
            ", isLocked=" + isLocked +
            '}';
    }
}
