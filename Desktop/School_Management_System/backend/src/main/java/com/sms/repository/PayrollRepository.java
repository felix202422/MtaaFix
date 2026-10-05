package com.sms.repository;

import com.sms.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    List<Payroll> findByTeacherId(Long teacherId);

    List<Payroll> findByMonthAndYear(Integer month, Integer year);
}
