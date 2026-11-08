package com.salescrm.service.impl;

import com.salescrm.domain.StaffAttendance;
import com.salescrm.repository.StaffAttendanceRepository;
import com.salescrm.repository.UserRepository;
import com.salescrm.service.StaffAttendanceService;
import com.salescrm.service.dto.StaffAttendanceDTO;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service Implementation for managing Staff Attendance.
 * Enforces server-side clock integrity, immutability of checked-out records, and zero-backdating.
 */
@Service
@Transactional
public class StaffAttendanceServiceImpl implements StaffAttendanceService {

    private static final Logger LOG = LoggerFactory.getLogger(StaffAttendanceServiceImpl.class);
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");
    private static final LocalTime SHIFT_GRACE_TIME = LocalTime.of(9, 30); // 9:30 AM IST

    private final StaffAttendanceRepository staffAttendanceRepository;
    private final UserRepository userRepository;

    public StaffAttendanceServiceImpl(
        StaffAttendanceRepository staffAttendanceRepository,
        UserRepository userRepository
    ) {
        this.staffAttendanceRepository = staffAttendanceRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Mono<StaffAttendanceDTO> checkIn(
        String userId,
        String userName,
        String userRole,
        String workMode,
        String note,
        String ipAddress,
        String deviceInfo
    ) {
        if (userId == null || userId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required for check-in"));
        }
        if ("admin".equalsIgnoreCase(userId.trim()) || "administrator".equalsIgnoreCase(userId.trim())) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Administrators do not log staff shift attendance."));
        }

        // Server-Side Clock Authority: Always determine date and time strictly on server in Asia/Kolkata
        LocalDate today = LocalDate.now(IST_ZONE);
        Instant now = Instant.now();

        return staffAttendanceRepository.findByUserIdAndAttendanceDate(userId, today)
            .flatMap(existing -> {
                LOG.info("User [{}] already has an existing check-in for today [{}] at {}. Punch is immutable.",
                    userId, today, existing.getCheckInTime());
                return Mono.just(toDTO(existing));
            })
            .switchIfEmpty(
                Mono.defer(() -> {
                    StaffAttendance attendance = new StaffAttendance();
                    attendance.setUserId(userId);
                    attendance.setUserName(userName != null && !userName.isBlank() ? userName : userId);
                    attendance.setUserRole(userRole != null && !userRole.isBlank() ? userRole : "STAFF");
                    attendance.setAttendanceDate(today);
                    attendance.setCheckInTime(now);
                    attendance.setWorkMode(workMode != null && !workMode.isBlank() ? workMode : "OFFICE");
                    attendance.setCheckInNote(note);
                    attendance.setIpAddress(ipAddress);
                    attendance.setDeviceInfo(deviceInfo);
                    attendance.setCreatedAt(now);
                    attendance.setUpdatedAt(now);
                    attendance.setTotalMinutes(0);

                    // Determine initial status based on shift time
                    ZonedDateTime istZoned = now.atZone(IST_ZONE);
                    if (istZoned.toLocalTime().isAfter(SHIFT_GRACE_TIME)) {
                        attendance.setStatus("LATE");
                    } else {
                        attendance.setStatus("PRESENT");
                    }

                    LOG.info("Recording check-in for user [{}] on [{}] with status [{}]", userId, today, attendance.getStatus());
                    return staffAttendanceRepository.save(attendance).map(this::toDTO);
                })
            );
    }

    @Override
    public Mono<StaffAttendanceDTO> checkOut(String userId, String note) {
        if (userId == null || userId.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "User ID is required for check-out"));
        }

        LocalDate today = LocalDate.now(IST_ZONE);
        Instant now = Instant.now();

        return staffAttendanceRepository.findByUserIdAndAttendanceDate(userId, today)
            .switchIfEmpty(
                Mono.error(new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot punch out: No active check-in record found for today (" + today + "). You must punch in before punching out."
                ))
            )
            .flatMap(attendance -> {
                // Anti-Forgery / Immutability Check: If checkout already occurred, prevent modifying or extending shift
                if (attendance.getCheckOutTime() != null) {
                    LOG.warn("SECURITY ALERT: User [{}] attempted to overwrite finalized check-out for date [{}]", userId, today);
                    return Mono.error(new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Shift Finalized: Today's shift was already checked out and locked at " + attendance.getCheckOutTime() + ". Tampering is prohibited."
                    ));
                }

                Instant inTime = attendance.getCheckInTime() != null ? attendance.getCheckInTime() : now;
                long minutes = Math.max(0, Duration.between(inTime, now).toMinutes());

                attendance.setCheckOutTime(now);
                attendance.setCheckOutNote(note);
                attendance.setUpdatedAt(now);
                attendance.setTotalMinutes((int) minutes);

                long hours = minutes / 60;
                long remMins = minutes % 60;
                attendance.setTotalHoursFormatted(hours + "h " + remMins + "m");

                // Evaluate final daily attendance classification
                if (minutes < 240) { // less than 4 hours
                    attendance.setStatus("HALF_DAY");
                } else if (minutes >= 540) { // 9+ hours
                    attendance.setStatus("OVERTIME");
                }

                LOG.info("Recording check-out for user [{}] with total hours [{}] and status [{}]",
                    userId, attendance.getTotalHoursFormatted(), attendance.getStatus());

                return staffAttendanceRepository.save(attendance).map(this::toDTO);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<StaffAttendanceDTO> getTodayAttendance(String userId) {
        if (userId == null || userId.isBlank()) return Mono.empty();
        LocalDate today = LocalDate.now(IST_ZONE);
        return staffAttendanceRepository.findByUserIdAndAttendanceDate(userId, today).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<StaffAttendanceDTO> getAttendanceHistory(String userId, LocalDate fromDate, LocalDate toDate) {
        if (userId == null || userId.isBlank()) return Flux.empty();
        LocalDate today = LocalDate.now(IST_ZONE);
        LocalDate start = fromDate != null ? fromDate : today.minusMonths(1);
        LocalDate end = toDate != null ? toDate : today;

        return staffAttendanceRepository
            .findAllByUserIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(userId, start, end)
            .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Flux<StaffAttendanceDTO> getAllAttendance(LocalDate fromDate, LocalDate toDate, String userIdFilter) {
        LocalDate today = LocalDate.now(IST_ZONE);
        LocalDate start = fromDate != null ? fromDate : today.minusDays(14);
        LocalDate end = toDate != null ? toDate : today;

        Flux<StaffAttendance> flux;
        if (userIdFilter != null && !userIdFilter.isBlank() && !"ALL".equalsIgnoreCase(userIdFilter)) {
            flux = staffAttendanceRepository.findAllByUserIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(userIdFilter, start, end);
        } else {
            flux = staffAttendanceRepository.findAllByAttendanceDateBetweenOrderByAttendanceDateDescCheckInTimeDesc(start, end);
        }

        return flux
            .filter(a -> a.getUserId() == null || (!"admin".equalsIgnoreCase(a.getUserId().trim()) && !"administrator".equalsIgnoreCase(a.getUserId().trim())))
            .map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Mono<Map<String, Object>> getAttendanceSummary(LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(IST_ZONE);

        return staffAttendanceRepository.findAllByAttendanceDateOrderByCheckInTimeDesc(targetDate)
            .filter(a -> a.getUserId() == null || (!"admin".equalsIgnoreCase(a.getUserId().trim()) && !"administrator".equalsIgnoreCase(a.getUserId().trim())))
            .collectList()
            .flatMap(list -> {
                long totalPresent = list.size();
                long currentlyWorking = list.stream().filter(a -> a.getCheckInTime() != null && a.getCheckOutTime() == null).count();
                long completedCheckout = list.stream().filter(a -> a.getCheckOutTime() != null).count();
                long lateCount = list.stream().filter(a -> "LATE".equalsIgnoreCase(a.getStatus())).count();
                long halfDayCount = list.stream().filter(a -> "HALF_DAY".equalsIgnoreCase(a.getStatus())).count();
                long overtimeCount = list.stream().filter(a -> "OVERTIME".equalsIgnoreCase(a.getStatus())).count();

                long totalMinutes = list.stream().mapToLong(a -> a.getTotalMinutes() != null ? a.getTotalMinutes() : 0).sum();
                double avgHours = completedCheckout > 0 ? ((double) totalMinutes / (double) completedCheckout) / 60.0 : 0.0;

                Map<String, Object> summary = new HashMap<>();
                summary.put("date", targetDate.toString());
                summary.put("totalPresent", totalPresent);
                summary.put("currentlyWorking", currentlyWorking);
                summary.put("completedCheckout", completedCheckout);
                summary.put("lateCount", lateCount);
                summary.put("halfDayCount", halfDayCount);
                summary.put("overtimeCount", overtimeCount);
                summary.put("averageHours", Math.round(avgHours * 10.0) / 10.0);

                return Mono.just(summary);
            });
    }

    private StaffAttendanceDTO toDTO(StaffAttendance entity) {
        if (entity == null) return null;
        StaffAttendanceDTO dto = new StaffAttendanceDTO();
        dto.setId(entity.getId());
        dto.setUserId(entity.getUserId());
        dto.setUserName(entity.getUserName());
        dto.setUserRole(entity.getUserRole());
        dto.setAttendanceDate(entity.getAttendanceDate());
        dto.setCheckInTime(entity.getCheckInTime());
        dto.setCheckOutTime(entity.getCheckOutTime());
        dto.setWorkMode(entity.getWorkMode());
        dto.setStatus(entity.getStatus());
        dto.setTotalMinutes(entity.getTotalMinutes());
        dto.setTotalHoursFormatted(entity.getTotalHoursFormatted());
        dto.setCheckInNote(entity.getCheckInNote());
        dto.setCheckOutNote(entity.getCheckOutNote());
        dto.setIpAddress(entity.getIpAddress());
        dto.setDeviceInfo(entity.getDeviceInfo());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
