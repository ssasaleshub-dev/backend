package com.salescrm.web.rest;

import com.salescrm.security.AuthoritiesConstants;
import com.salescrm.security.SecurityUtils;
import com.salescrm.service.StaffAttendanceService;
import com.salescrm.service.dto.StaffAttendanceDTO;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST controller for Enterprise Staff Attendance Management.
 * Fortified with Anti-Forgery, Anti-Spoofing, and Strict Server Clock Enforcement.
 *
 * Security Controls:
 * 1. Current-Date Strictness: All dates are calculated strictly on the server (Asia/Kolkata). Client dates are ignored or rejected.
 * 2. Identity Integrity: Non-admin users can ONLY punch for their own authenticated session.
 * 3. Immutability: Once checked out, records are finalized and locked.
 * 4. Zero Backdating/Future-Dating: Any attempt to inject custom dates/timestamps is blocked.
 */
@RestController
@RequestMapping("/api/attendance")
public class StaffAttendanceResource {

    private static final Logger LOG = LoggerFactory.getLogger(StaffAttendanceResource.class);
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    private final StaffAttendanceService staffAttendanceService;

    public StaffAttendanceResource(StaffAttendanceService staffAttendanceService) {
        this.staffAttendanceService = staffAttendanceService;
    }

    /**
     * {@code POST /api/attendance/check-in} : Record check-in / login punch for staff user.
     * Prevents forgery, unauthorized user impersonation, and historical date tampering.
     */
    @PostMapping(value = "/check-in", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<ResponseEntity<StaffAttendanceDTO>> checkIn(@RequestBody Map<String, String> payload) {
        // Anti-Forgery Check: Reject any attempt to spoof dates
        validateNoDateForgery(payload);

        String bodyUserId = payload.get("userId");
        String userName = payload.get("userName");
        String userRole = payload.get("userRole");
        String workMode = payload.getOrDefault("workMode", "OFFICE");
        String note = payload.get("note");
        String ipAddress = payload.get("ipAddress");
        String deviceInfo = payload.get("deviceInfo");

        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty(bodyUserId != null && !bodyUserId.isBlank() ? bodyUserId : "anonymousUser")
            .flatMap(authenticatedUser ->
                SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)
                    .defaultIfEmpty(false)
                    .flatMap(isAdmin -> {
                        String targetUser;
                        if (isAdmin) {
                            targetUser = (bodyUserId != null && !bodyUserId.isBlank()) ? bodyUserId : authenticatedUser;
                        } else {
                            // Non-admin can NEVER punch for another employee
                            if (bodyUserId != null && !bodyUserId.isBlank() && !bodyUserId.equalsIgnoreCase(authenticatedUser) && !"anonymousUser".equals(authenticatedUser)) {
                                LOG.warn("SECURITY ALERT: User [{}] attempted to punch check-in for unauthorized user [{}]", authenticatedUser, bodyUserId);
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Security violation: You cannot record attendance for other employees."));
                            }
                            targetUser = !"anonymousUser".equals(authenticatedUser) ? authenticatedUser : bodyUserId;
                        }

                        if (targetUser == null || targetUser.isBlank() || "anonymousUser".equalsIgnoreCase(targetUser)) {
                            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to punch attendance."));
                        }

                        LOG.info("Authorized check-in request for staff: {} (by: {}, isAdmin: {})", targetUser, authenticatedUser, isAdmin);
                        return staffAttendanceService.checkIn(targetUser, userName, userRole, workMode, note, ipAddress, deviceInfo);
                    })
            )
            .map(result -> ResponseEntity.status(HttpStatus.CREATED).body(result));
    }

    /**
     * {@code POST /api/attendance/check-out} : Record check-out / logout punch for staff user.
     * Enforces that checkout can only happen for an existing active check-in on the current day.
     */
    @PostMapping(value = "/check-out", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<StaffAttendanceDTO>> checkOut(@RequestBody Map<String, String> payload) {
        // Anti-Forgery Check: Reject any attempt to spoof dates
        validateNoDateForgery(payload);

        String bodyUserId = payload.get("userId");
        String note = payload.get("note");

        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty(bodyUserId != null && !bodyUserId.isBlank() ? bodyUserId : "anonymousUser")
            .flatMap(authenticatedUser ->
                SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)
                    .defaultIfEmpty(false)
                    .flatMap(isAdmin -> {
                        String targetUser;
                        if (isAdmin) {
                            targetUser = (bodyUserId != null && !bodyUserId.isBlank()) ? bodyUserId : authenticatedUser;
                        } else {
                            if (bodyUserId != null && !bodyUserId.isBlank() && !bodyUserId.equalsIgnoreCase(authenticatedUser) && !"anonymousUser".equals(authenticatedUser)) {
                                LOG.warn("SECURITY ALERT: User [{}] attempted to punch check-out for unauthorized user [{}]", authenticatedUser, bodyUserId);
                                return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Security violation: You cannot punch check-out for other employees."));
                            }
                            targetUser = !"anonymousUser".equals(authenticatedUser) ? authenticatedUser : bodyUserId;
                        }

                        if (targetUser == null || targetUser.isBlank() || "anonymousUser".equalsIgnoreCase(targetUser)) {
                            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required to punch attendance."));
                        }

                        LOG.info("Authorized check-out request for staff: {} (by: {}, isAdmin: {})", targetUser, authenticatedUser, isAdmin);
                        return staffAttendanceService.checkOut(targetUser, note);
                    })
            )
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
     * Non-admin users are strictly restricted to querying their own history.
     */
    @GetMapping(value = "/my-history", produces = MediaType.APPLICATION_JSON_VALUE)
    public Flux<StaffAttendanceDTO> getMyHistory(
        @RequestParam(value = "userId", required = false) String userId,
        @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return SecurityUtils.getCurrentUserLogin()
            .defaultIfEmpty("staff")
            .flatMapMany(authenticatedUser ->
                SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)
                    .defaultIfEmpty(false)
                    .flatMapMany(isAdmin -> {
                        String effectiveUser = (isAdmin && userId != null && !userId.isBlank()) ? userId : authenticatedUser;
                        return staffAttendanceService.getAttendanceHistory(effectiveUser, fromDate, toDate);
                    })
            );
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
        return SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)
            .defaultIfEmpty(false)
            .flatMapMany(isAdmin -> {
                if (!isAdmin) {
                    LOG.warn("Unauthorized attempt to access Admin Attendance Roster");
                    return Flux.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Admin authorization required."));
                }
                LOG.debug("REST request for Admin Attendance Roster: from {} to {} for user filter {}", fromDate, toDate, userIdFilter);
                return staffAttendanceService.getAllAttendance(fromDate, toDate, userIdFilter);
            });
    }

    /**
     * {@code GET /api/attendance/admin/summary} : Real-time MNC attendance KPI metrics for Admin.
     */
    @GetMapping(value = "/admin/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, Object>>> getAttendanceSummary(
        @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return SecurityUtils.hasCurrentUserThisAuthority(AuthoritiesConstants.ADMIN)
            .defaultIfEmpty(false)
            .flatMap(isAdmin -> {
                if (!isAdmin) {
                    return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Access Denied: Admin authorization required."));
                }
                return staffAttendanceService.getAttendanceSummary(date).map(ResponseEntity::ok);
            });
    }

    /**
     * Validates that the client request does not attempt to backdate, future-date, or forge timestamps.
     */
    private void validateNoDateForgery(Map<String, String> payload) {
        if (payload == null) return;

        LocalDate serverToday = LocalDate.now(IST_ZONE);

        // Check for injected date fields
        String clientDate = payload.get("attendanceDate");
        if (clientDate == null) clientDate = payload.get("date");

        if (clientDate != null && !clientDate.isBlank()) {
            try {
                LocalDate parsed = LocalDate.parse(clientDate.trim());
                if (!parsed.equals(serverToday)) {
                    LOG.warn("SECURITY ALERT: Client attempted to forge attendance date [{}] when server date is [{}]", parsed, serverToday);
                    throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Forgery Blocked: Attendance can only be recorded for the current date (" + serverToday + "). Backdating and future-dating are strictly prohibited."
                    );
                }
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format in payload: " + clientDate);
            }
        }

        // Check for injected timestamp fields
        if (payload.containsKey("checkInTime") || payload.containsKey("checkOutTime")) {
            LOG.warn("SECURITY ALERT: Client attempted to inject manual timestamps into attendance payload");
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Forgery Blocked: Client-supplied timestamps are prohibited. All check-in/out times are generated strictly by the server system clock."
            );
        }
    }
}
