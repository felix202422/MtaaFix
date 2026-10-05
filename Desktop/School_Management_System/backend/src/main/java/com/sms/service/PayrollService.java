package com.sms.service;

import com.sms.entity.Payroll;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PayrollService {

    Payroll addPayroll(Payroll payroll);

    Payroll updatePayroll(Long id, Payroll payroll);

    Payroll getPayrollById(Long id);

    Page<Payroll> getAllPayroll(Pageable pageable);

    List<Payroll> getByTeacher(Long teacherId);

    List<Payroll> getByMonthAndYear(Integer month, Integer year);

    Payroll processPayment(Long id);

    void deletePayroll(Long id);
}
