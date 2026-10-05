package com.sms.service.impl;

import com.sms.entity.Attendance;
import com.sms.repository.AttendanceRepository;
import com.sms.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;

    @Override
    public Attendance markAttendance(Attendance attendance) {
        return attendanceRepository.save(attendance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceByStudentAndDateRange(Long studentId, LocalDate start, LocalDate end) {
        return attendanceRepository.findByStudentIdAndDateBetween(studentId, start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> getAttendanceByClassAndDate(Long classId, LocalDate date) {
        return attendanceRepository.findByClassRoomIdAndDate(classId, date);
    }

    @Override
    public List<Attendance> markBulkAttendance(List<Attendance> attendances) {
        return attendanceRepository.saveAll(attendances);
    }

    @Override
    public long getAttendanceCountByStatusAndDateRange(Long studentId, String status, LocalDate start, LocalDate end) {
        return attendanceRepository.countByStudentIdAndStatusAndDateBetween(studentId, status, start, end);
    }

    public AttendanceServiceImpl(AttendanceRepository attendanceRepository) {
        this.attendanceRepository = attendanceRepository;
    }

}
