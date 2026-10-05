package com.sms.repository;

import com.sms.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {

    Optional<Teacher> findByEmployeeNumber(String employeeNumber);

    Optional<Teacher> findByUserId(Long userId);

    List<Teacher> findByDepartmentId(Long departmentId);

    Long countByEmploymentStatus(String status);
}
