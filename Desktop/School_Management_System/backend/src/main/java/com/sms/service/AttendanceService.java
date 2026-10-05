package com.sms.service;

import com.sms.entity.Attendance;
import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    Attendance markAttendance(Attendance attendance);
    List<Attendance> getAttendanceByStudentAndDateRange(Long studentId, LocalDate start, LocalDate end);
    List<Attendance> getAttendanceByClassAndDate(Long classId, LocalDate date);
    List<Attendance> markBulkAttendance(List<Attendance> attendances);
    long getAttendanceCountByStatusAndDateRange(Long studentId, String status, LocalDate start, LocalDate end);
}
