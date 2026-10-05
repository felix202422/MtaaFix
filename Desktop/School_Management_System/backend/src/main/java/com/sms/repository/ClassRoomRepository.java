package com.sms.repository;

import com.sms.entity.ClassRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClassRoomRepository extends JpaRepository<ClassRoom, Long> {

    List<ClassRoom> findByDepartmentId(Long departmentId);

    List<ClassRoom> findByAcademicYearId(Long academicYearId);

    Optional<ClassRoom> findByNameAndSection(String name, String section);
}
