package com.sms.repository;

import com.sms.entity.Examination;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExaminationRepository extends JpaRepository<Examination, Long> {

    List<Examination> findByTermId(Long termId);

    List<Examination> findByAcademicYearId(Long academicYearId);
}
