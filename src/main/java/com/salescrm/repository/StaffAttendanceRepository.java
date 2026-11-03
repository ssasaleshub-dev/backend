package com.salescrm.repository;

import com.salescrm.domain.StaffAttendance;
import java.time.LocalDate;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface StaffAttendanceRepository extends ReactiveCrudRepository<StaffAttendance, Long> {

    Mono<StaffAttendance> findByUserIdAndAttendanceDate(String userId, LocalDate attendanceDate);

    Flux<StaffAttendance> findAllByUserIdOrderByAttendanceDateDesc(String userId);

    Flux<StaffAttendance> findAllByUserIdAndAttendanceDateBetweenOrderByAttendanceDateDesc(
        String userId,
        LocalDate fromDate,
        LocalDate toDate
    );

    Flux<StaffAttendance> findAllByAttendanceDateOrderByCheckInTimeDesc(LocalDate attendanceDate);

    Flux<StaffAttendance> findAllByAttendanceDateBetweenOrderByAttendanceDateDescCheckInTimeDesc(
        LocalDate fromDate,
        LocalDate toDate
    );

    Mono<Long> countByAttendanceDate(LocalDate attendanceDate);

    Mono<Long> countByAttendanceDateAndStatus(LocalDate attendanceDate, String status);

    @Query("SELECT COUNT(*) FROM staff_attendance WHERE attendance_date = :attendanceDate AND check_in_time IS NOT NULL AND check_out_time IS NULL")
    Mono<Long> countCurrentlyWorking(LocalDate attendanceDate);
}

