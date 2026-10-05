package com.sms.service;

import com.sms.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

public interface StudentService {
    Page<Student> getAllStudents(Pageable pageable);
    Student getStudentById(Long id);
    Student getStudentByAdmissionNumber(String admissionNumber);
    Student createStudent(Student student);
    Student updateStudent(Long id, Student student);
    void deleteStudent(Long id);
    Page<Student> searchStudents(String keyword, Pageable pageable);
    List<Student> getStudentsByClassId(Long classId);
    List<Student> getStudentsByParentId(Long parentId);
    long getStudentCount();
    long getActiveStudentCount();
}
