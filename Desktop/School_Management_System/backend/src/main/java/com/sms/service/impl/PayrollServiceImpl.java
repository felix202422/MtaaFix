package com.sms.service.impl;

import com.sms.entity.Payroll;
import com.sms.exception.BadRequestException;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.PayrollRepository;
import com.sms.service.PayrollService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;

    @Override
    public Payroll addPayroll(Payroll payroll) {
        if (payroll.getStatus() == null) {
            payroll.setStatus("PENDING");
        }
        payroll.setNetSalary(computeNet(payroll));
        return payrollRepository.save(payroll);
    }

    @Override
    public Payroll updatePayroll(Long id, Payroll payroll) {
        getPayrollById(id);
        payroll.setId(id);
        payroll.setNetSalary(computeNet(payroll));
        return payrollRepository.save(payroll);
    }

    @Override
    @Transactional(readOnly = true)
    public Payroll getPayrollById(Long id) {
        return payrollRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll record not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Payroll> getAllPayroll(Pageable pageable) {
        return payrollRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payroll> getByTeacher(Long teacherId) {
        return payrollRepository.findByTeacherId(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Payroll> getByMonthAndYear(Integer month, Integer year) {
        return payrollRepository.findByMonthAndYear(month, year);
    }

    @Override
    public Payroll processPayment(Long id) {
        Payroll payroll = getPayrollById(id);
        if ("PAID".equals(payroll.getStatus())) {
            throw new BadRequestException("Payroll already processed");
        }
        payroll.setStatus("PAID");
        return payrollRepository.save(payroll);
    }

    @Override
    public void deletePayroll(Long id) {
        if (!payrollRepository.existsById(id)) {
            throw new ResourceNotFoundException("Payroll record not found with id: " + id);
        }
        payrollRepository.deleteById(id);
    }

    private BigDecimal computeNet(Payroll payroll) {
        BigDecimal basic = payroll.getBasicSalary() == null ? BigDecimal.ZERO : payroll.getBasicSalary();
        BigDecimal allowances = payroll.getAllowances() == null ? BigDecimal.ZERO : payroll.getAllowances();
        BigDecimal deductions = payroll.getDeductions() == null ? BigDecimal.ZERO : payroll.getDeductions();
        BigDecimal tax = payroll.getTax() == null ? BigDecimal.ZERO : payroll.getTax();
        return basic.add(allowances).subtract(deductions).subtract(tax);
    }

    public PayrollServiceImpl(PayrollRepository payrollRepository) {
        this.payrollRepository = payrollRepository;
    }

}
