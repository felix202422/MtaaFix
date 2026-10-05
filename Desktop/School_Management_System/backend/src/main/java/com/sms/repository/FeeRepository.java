package com.sms.repository;

import com.sms.entity.Fee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeeRepository extends JpaRepository<Fee, Long> {

    List<Fee> findByClassRoomId(Long classRoomId);

    List<Fee> findByAcademicYearId(Long academicYearId);
}
