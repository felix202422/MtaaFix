package com.sms.repository;

import com.sms.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    List<Attendance> findByStudentIdAndDateBetween(Long studentId, LocalDate start, LocalDate end);

    List<Attendance> findByClassRoomIdAndDate(Long classRoomId, LocalDate date);

    Long countByStudentIdAndStatusAndDateBetween(Long studentId, String status, LocalDate start, LocalDate end);

    Long countByStudentIdAndDateBetween(Long studentId, LocalDate start, LocalDate end);

    Optional<Attendance> findByStudentIdAndClassRoomIdAndDate(Long studentId, Long classRoomId, LocalDate date);

    @Query("SELECT MAX(a.date) FROM Attendance a")
    LocalDate findMaxDate();

    @Query(value = "SELECT CAST(a.date AS TEXT), ROUND(100.0 * SUM(CASE WHEN a.status = 'PRESENT' THEN 1 ELSE 0 END) / COUNT(*), 1) " +
            "FROM attendance a WHERE a.date >= :start " +
            "GROUP BY a.date ORDER BY a.date", nativeQuery = true)
    List<Object[]> findDailyAttendanceRate(@Param("start") LocalDate start);

    @Query("SELECT a.status, COUNT(a) FROM Attendance a GROUP BY a.status")
    List<Object[]> countByStatus();
}
