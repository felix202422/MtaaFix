package com.sms.repository;

import com.sms.entity.TeacherSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherSubjectRepository extends JpaRepository<TeacherSubject, Long> {

    List<TeacherSubject> findByTeacherId(Long teacherId);

    List<TeacherSubject> findBySubjectId(Long subjectId);

    List<TeacherSubject> findByClassRoomId(Long classRoomId);

    List<TeacherSubject> findByTeacherIdAndAcademicYearId(Long teacherId, Long academicYearId);
}
