package com.sms.repository;

import com.sms.entity.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {

    List<ExamResult> findByExaminationId(Long examinationId);

    List<ExamResult> findByStudentId(Long studentId);

    List<ExamResult> findByExaminationIdAndStudentId(Long examinationId, Long studentId);

    Optional<ExamResult> findByExaminationIdAndStudentIdAndSubjectId(Long examinationId, Long studentId, Long subjectId);

    @Query("SELECT r.grade, COUNT(r) FROM ExamResult r GROUP BY r.grade")
    List<Object[]> countByGrade();
}
