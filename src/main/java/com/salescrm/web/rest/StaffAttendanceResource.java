package com.salescrm.web.rest;

import com.salescrm.security.SecurityUtils;
import com.salescrm.service.StaffAttendanceService;
import com.salescrm.service.dto.StaffAttendanceDTO;
import java.time.LocalDate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for MNC Enterprise Staff Attendance Management.
 * Supports:
 * - Daily staff check-in (Login time + office/remote mode + note)
 * - Daily staff check-out (Logout time + total hours calculation)
 * - Real-time "Today" attendance status
 * - Individual counselor monthly attendance history
 * - Dynamic Admin view for all staff attendance and real-time enterprise metrics
 */
@RestController
@RequestMapping("/api/attendance")
public class StaffAttendanceResource {

    private static final Logger LOG = LoggerFactory.getLogger(StaffAttendanceResource.class);

    private final StaffAttendanceService staffAttendanceService;

    public StaffAttendanceResource(StaffAttendanceService staffAttendanceService) {
        this.staffAttendanceService = staffAttendanceService;
    }

    /**
     * {@code POST /api/attendance/check-in} : Record check-in / login punch for staff user.
     */
    @PostMapping(value = "/check-in", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<StaffAttendanceDTO>> checkIn(@RequestBody Map<String, String> payload) {
        String bodyUserId = payload.get("userId");
        String userName = payload.get("userName");
        String userRole = payload.get("userRole");
        String workMode = payload.getOrDefault("workMode", "OFFICE");
        String note = payload.get("note");
        String ipAddress = payload.get("ipAddress");
        String deviceInfo = payload.get("deviceInfo");

        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty(bodyUserId != null && !bodyUserId.isBlank() ? bodyUserId : "staff")
            .flatMap(resolvedUser -> {
                String finalUser = (bodyUserId != null && !bodyUserId.isBlank()) ? bodyUserId : resolvedUser;
                LOG.debug("REST request to check-in for staff: {}", finalUser);
                return staffAttendanceService.checkIn(finalUser, userName, userRole, workMode, note, ipAddress, deviceInfo);
            })
            .map(result -> ResponseEntity.status(HttpStatus.CREATED).body(result));
    }

    /**
     * {@code POST /api/attendance/check-out} : Record check-out / logout punch for staff user.
     */
    @PostMapping(value = "/check-out", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<StaffAttendanceDTO>> checkOut(@RequestBody Map<String, String> payload) {
        String bodyUserId = payload.get("userId");
        String note = payload.get("note");

        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty(bodyUserId != null && !bodyUserId.isBlank() ? bodyUserId : "staff")
            .flatMap(resolvedUser -> {
                String finalUser = (bodyUserId != null && !bodyUserId.isBlank()) ? bodyUserId : resolvedUser;
                LOG.debug("REST request to check-out for staff: {}", finalUser);
                return staffAttendanceService.checkOut(finalUser, note);
            })
            .map(ResponseEntity::ok);
    }

    /**
     * {@code GET /api/attendance/today} : Get today's attendance record for staff user.
     */
    @GetMapping(value = "/today", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<StaffAttendanceDTO>> getTodayAttendance(
        @RequestParam(value = "userId", required = false) String userId
    ) {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty(userId != null && !userId.isBlank() ? userId : "staff")
            .flatMap(resolvedUser -> {
                String finalUser = (userId != null && !userId.isBlank()) ? userId : resolvedUser;
                return staffAttendanceService.getTodayAttendance(finalUser);
            })
            .map(ResponseEntity::ok)
            .defaultIfEmpty(ResponseEntity.noContent().build());
    }

    /**
     * {@code GET /api/attendance/my-history} : Get attendance history for counselor profile.
     */
    @GetMapping(value = "/my-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<StaffAttendanceDTO> getMyHistory(
        @RequestParam(value = "userId", required = false) String userId,
        @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        String effectiveUser = userId != null && !userId.isBlank() ? userId : "staff";
        return staffAttendanceService.getAttendanceHistory(effectiveUser, fromDate, toDate);
    }

    /**
     * {@code GET /api/attendance/admin/all} : MNC-level roster of all staff attendance records for Admin.
     */
    @GetMapping(value = "/admin/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<StaffAttendanceDTO> getAllAttendanceAdmin(
        @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
        @RequestParam(value = "userIdFilter", required = false) String userIdFilter
    ) {
        LOG.debug("REST request for Admin Attendance Roster: from {} to {} for user filter {}", fromDate, toDate, userIdFilter);
        return staffAttendanceService.getAllAttendance(fromDate, toDate, userIdFilter);
    }

    /**
     * {@code GET /api/attendance/admin/summary} : Real-time MNC attendance KPI metrics for Admin.
     */
    @GetMapping(value = "/admin/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getAttendanceSummary(
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return staffAttendanceService.getAttendanceSummary(date).map(ResponseEntity::ok);
    }
}

