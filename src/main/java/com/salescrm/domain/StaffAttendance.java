package com.salescrm.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Enterprise MNC-grade Staff Daily Attendance domain entity.
 * Records check-in / check-out times, work modes, automated status (PRESENT, LATE, HALF_DAY, OVERTIME),
 * and total duration for counselors and staff users.
 */
@Table("staff_attendance")
public class StaffAttendance implements Serializable {

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

    @Size(max = 50)
    @Column("user_role")
    private String userRole;

    @NotNull(message = "must not be null")
    @Column("attendance_date")
    private LocalDate attendanceDate;

    @Column("check_in_time")
    private Instant checkInTime;

    @Column("check_out_time")
    private Instant checkOutTime;

    @NotNull(message = "must not be null")
    @Size(max = 50)
    @Column("work_mode")
    private String workMode = "OFFICE"; // OFFICE, REMOTE, FIELD

    @NotNull(message = "must not be null")
    @Size(max = 50)
    @Column("status")
    private String status = "PRESENT"; // PRESENT, LATE, HALF_DAY, OVERTIME, ON_DUTY, ABSENT

    @Column("total_minutes")
    private Integer totalMinutes = 0;

    @Size(max = 50)
    @Column("total_hours_formatted")
    private String totalHoursFormatted; // e.g. "8h 30m"

    @Size(max = 500)
    @Column("check_in_note")
    private String checkInNote;

    @Size(max = 500)
    @Column("check_out_note")
    private String checkOutNote;

    @Size(max = 100)
    @Column("ip_address")
    private String ipAddress;

    @Size(max = 255)
    @Column("device_info")
    private String deviceInfo;

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

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public Instant getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(Instant checkInTime) {
        this.checkInTime = checkInTime;
    }

    public Instant getCheckOutTime() {
        return checkOutTime;
    }

    public void setCheckOutTime(Instant checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public String getWorkMode() {
        return workMode;
    }

    public void setWorkMode(String workMode) {
        this.workMode = workMode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalMinutes() {
        return totalMinutes;
    }

    public void setTotalMinutes(Integer totalMinutes) {
        this.totalMinutes = totalMinutes;
    }

    public String getTotalHoursFormatted() {
        return totalHoursFormatted;
    }

    public void setTotalHoursFormatted(String totalHoursFormatted) {
        this.totalHoursFormatted = totalHoursFormatted;
    }

    public String getCheckInNote() {
        return checkInNote;
    }

    public void setCheckInNote(String checkInNote) {
        this.checkInNote = checkInNote;
    }

    public String getCheckOutNote() {
        return checkOutNote;
    }

    public void setCheckOutNote(String checkOutNote) {
        this.checkOutNote = checkOutNote;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
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
        if (!(o instanceof StaffAttendance that)) return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        return "StaffAttendance{" +
            "id=" + id +
            ", userId='" + userId + '\'' +
            ", userName='" + userName + '\'' +
            ", attendanceDate=" + attendanceDate +
            ", checkInTime=" + checkInTime +
            ", checkOutTime=" + checkOutTime +
            ", workMode='" + workMode + '\'' +
            ", status='" + status + '\'' +
            ", totalMinutes=" + totalMinutes +
            ", totalHoursFormatted='" + totalHoursFormatted + '\'' +
            '}';
    }
}

