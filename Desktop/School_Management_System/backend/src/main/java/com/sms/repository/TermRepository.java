package com.sms.repository;

import com.sms.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermRepository extends JpaRepository<Term, Long> {

    Optional<Term> findByIsCurrentTrue();

    List<Term> findByAcademicYearId(Long academicYearId);
}
