package com.sms.repository;

import com.sms.entity.StudentClass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentClassRepository extends JpaRepository<StudentClass, Long> {

    List<StudentClass> findByStudentId(Long studentId);

    List<StudentClass> findByClassRoomId(Long classRoomId);

    List<StudentClass> findByAcademicYearId(Long academicYearId);
}
