package com.sms.service.impl;

import com.sms.entity.*;
import com.sms.repository.*;
import com.sms.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final StudentRepository studentRepository;
    private final TeacherRepository teacherRepository;
    private final AttendanceRepository attendanceRepository;
    private final PaymentRepository paymentRepository;
    private final ClassRoomRepository classRoomRepository;
    private final EventRepository eventRepository;
    private final NotificationRepository notificationRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final ExamResultRepository examResultRepository;
    private final ParentRepository parentRepository;
    private final TeacherSubjectRepository teacherSubjectRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExaminationRepository examinationRepository;

    @Override
    public Map<String, Object> getAdminDashboard() {
        Map<String, Object> data = new HashMap<>();
        data.put("totalStudents", studentRepository.count());
        data.put("activeStudents", studentRepository.countByAcademicStatus("ACTIVE"));
        data.put("totalTeachers", teacherRepository.count());
        data.put("activeTeachers", teacherRepository.countByEmploymentStatus("ACTIVE"));
        data.put("totalClasses", classRoomRepository.count());
        data.put("totalPayments", paymentRepository.count());
        data.put("activeBorrows", borrowRecordRepository.findByStatus("BORROWED").size());
        data.put("upcomingEvents", eventRepository.findByStartDateBetween(
                java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now().plusDays(7)).size());
        data.put("charts", buildAdminCharts());
        return data;
    }

    /**
     * Aggregates for the admin dashboard charts: gender distribution (donut),
     * attendance status split (pie), daily attendance trend (area), monthly
     * fee revenue (bar), class sizes (line) and grade distribution (bar).
     */
    private Map<String, Object> buildAdminCharts() {
        Map<String, Object> charts = new HashMap<>();

        List<Map<String, Object>> gender = new ArrayList<>();
        for (Object[] row : studentRepository.countByGender()) {
            gender.add(rowToMap("name", row[0], "value", row[1]));
        }
        charts.put("genderDistribution", gender);

        List<Map<String, Object>> attendanceSplit = new ArrayList<>();
        for (Object[] row : attendanceRepository.countByStatus()) {
            attendanceSplit.add(rowToMap("name", row[0], "value", row[1]));
        }
        charts.put("attendanceDistribution", attendanceSplit);

        LocalDate maxDate = attendanceRepository.findMaxDate();
        LocalDate start = (maxDate != null ? maxDate : LocalDate.now()).minusDays(29);
        List<Map<String, Object>> trend = new ArrayList<>();
        for (Object[] row : attendanceRepository.findDailyAttendanceRate(start)) {
            trend.add(rowToMap("date", row[0], "rate", row[1]));
            if (trend.size() >= 30) break;
        }
        charts.put("attendanceTrend", trend);

        List<Map<String, Object>> revenue = new ArrayList<>();
        for (Object[] row : paymentRepository.sumMonthlyRevenue()) {
            revenue.add(rowToMap("month", row[0], "amount", row[1]));
        }
        charts.put("monthlyRevenue", revenue);

        List<Map<String, Object>> classSizes = new ArrayList<>();
        for (Object[] row : studentRepository.countStudentsPerClass()) {
            classSizes.add(rowToMap("name", row[0], "students", row[1]));
        }
        charts.put("classSizes", classSizes);

        List<Map<String, Object>> grades = new ArrayList<>();
        for (Object[] row : examResultRepository.countByGrade()) {
            grades.add(rowToMap("grade", row[0], "count", row[1]));
        }
        charts.put("gradeDistribution", grades);

        return charts;
    }

    @Override
    public Map<String, Object> getTeacherDashboard(Long userId) {
        Map<String, Object> data = new HashMap<>();
        Teacher teacher = teacherRepository.findByUserId(userId).orElse(null);
        if (teacher == null) {
            data.put("error", "No teacher profile linked to this account");
            return data;
        }
        Long teacherId = teacher.getId();
        data.put("teacherId", teacherId);
        data.put("name", teacher.getUser().getFullName());
        data.put("employeeNumber", teacher.getEmployeeNumber());
        data.put("classesCount", teacherSubjectRepository.findByTeacherId(teacherId).stream()
                .map(TeacherSubject::getClassRoom).map(ClassRoom::getId).distinct().count());
        data.put("assignmentsCount", assignmentRepository.findByTeacherId(teacherId).size());
        data.put("subjectsCount", teacherSubjectRepository.findByTeacherId(teacherId).stream()
                .map(TeacherSubject::getSubject).map(Subject::getId).distinct().count());
        return data;
    }

    @Override
    public Map<String, Object> getStudentDashboard(Long userId) {
        Map<String, Object> data = new HashMap<>();
        Student student = studentRepository.findByUserId(userId).orElse(null);
        if (student == null) {
            data.put("error", "No student profile linked to this account");
            return data;
        }
        Long studentId = student.getId();
        data.put("studentId", studentId);
        data.put("name", student.getUser().getFullName());
        data.put("admissionNumber", student.getAdmissionNumber());
        data.put("className", student.getCurrentClass() != null ? student.getCurrentClass().getName() : null);

        LocalDate maxDate = attendanceRepository.findMaxDate();
        LocalDate end = maxDate != null ? maxDate : LocalDate.now();
        LocalDate start = end.minusDays(29);
        long present = attendanceRepository.countByStudentIdAndStatusAndDateBetween(studentId, "PRESENT", start, end);
        long total = attendanceRepository.countByStudentIdAndDateBetween(studentId, start, end);
        data.put("attendanceRate", total > 0 ? Math.round(100.0 * present / total) : null);

        Long classId = student.getCurrentClass() != null ? student.getCurrentClass().getId() : -1L;
        data.put("assignmentsDue", assignmentRepository.findByClassRoomIdAndDueDateAfter(classId, java.time.LocalDateTime.now()).size());
        data.put("resultsCount", examResultRepository.findByStudentId(studentId).size());
        return data;
    }

    @Override
    public Map<String, Object> getParentDashboard(Long userId) {
        Map<String, Object> data = new HashMap<>();
        Parent parent = parentRepository.findByUserId(userId).orElse(null);
        if (parent == null) {
            data.put("error", "No parent profile linked to this account");
            return data;
        }
        data.put("parentId", parent.getId());
        List<Student> children = studentRepository.findByParentId(parent.getId());
        List<Map<String, Object>> childrenList = new ArrayList<>();
        for (Student s : children) {
            Map<String, Object> child = new LinkedHashMap<>();
            child.put("id", s.getId());
            child.put("name", s.getUser().getFullName());
            child.put("admissionNumber", s.getAdmissionNumber());
            child.put("className", s.getCurrentClass() != null ? s.getCurrentClass().getName() : "");
            childrenList.add(child);
        }
        data.put("children", children.size());
        data.put("childrenList", childrenList);
        return data;
    }

    private Map<String, Object> rowToMap(Object k1, Object v1, Object k2, Object v2) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(k1.toString(), v1);
        m.put(k2.toString(), v2);
        return m;
    }

    public DashboardServiceImpl(StudentRepository studentRepository, TeacherRepository teacherRepository, AttendanceRepository attendanceRepository, PaymentRepository paymentRepository, ClassRoomRepository classRoomRepository, EventRepository eventRepository, NotificationRepository notificationRepository, BorrowRecordRepository borrowRecordRepository, ExamResultRepository examResultRepository, ParentRepository parentRepository, TeacherSubjectRepository teacherSubjectRepository, AssignmentRepository assignmentRepository, ExaminationRepository examinationRepository) {
        this.studentRepository = studentRepository;
        this.teacherRepository = teacherRepository;
        this.attendanceRepository = attendanceRepository;
        this.paymentRepository = paymentRepository;
        this.classRoomRepository = classRoomRepository;
        this.eventRepository = eventRepository;
        this.notificationRepository = notificationRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.examResultRepository = examResultRepository;
        this.parentRepository = parentRepository;
        this.teacherSubjectRepository = teacherSubjectRepository;
        this.assignmentRepository = assignmentRepository;
        this.examinationRepository = examinationRepository;
    }
}
