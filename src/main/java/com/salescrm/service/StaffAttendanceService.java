package com.salescrm.service;

import com.salescrm.service.dto.StaffAttendanceDTO;
import java.time.LocalDate;
import java.util.Map;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface StaffAttendanceService {

    Mono<StaffAttendanceDTO> checkIn(
        String userId,
        String userName,
        String userRole,
        String workMode,
        String note,
        String ipAddress,
        String deviceInfo
    );

    Mono<StaffAttendanceDTO> checkOut(String userId, String note);

    Mono<StaffAttendanceDTO> getTodayAttendance(String userId);

    Flux<StaffAttendanceDTO> getAttendanceHistory(String userId, LocalDate fromDate, LocalDate toDate);

    Flux<StaffAttendanceDTO> getAllAttendance(LocalDate fromDate, LocalDate toDate, String userIdFilter);

    Mono<Map<String, Object>> getAttendanceSummary(LocalDate date);
}

