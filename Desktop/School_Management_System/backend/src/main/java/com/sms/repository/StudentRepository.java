package com.sms.repository;

import com.sms.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Long> {

    Optional<Student> findByAdmissionNumber(String admissionNumber);

    Optional<Student> findByUserId(Long userId);

    List<Student> findByCurrentClassId(Long classId);

    List<Student> findByParentId(Long parentId);

    Long countByCurrentClassId(Long classId);

    Long countByAcademicStatus(String status);

    @Query("SELECT s.gender, COUNT(s) FROM Student s GROUP BY s.gender")
    List<Object[]> countByGender();

    @Query(value = "SELECT c.name, COUNT(s.id) FROM classes c LEFT JOIN students s ON s.current_class_id = c.id " +
            "GROUP BY c.id, c.name ORDER BY c.id", nativeQuery = true)
    List<Object[]> countStudentsPerClass();

    @Query("SELECT s FROM Student s WHERE LOWER(CONCAT(s.user.firstName, ' ', s.user.lastName)) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Student> searchStudents(@Param("search") String search, Pageable pageable);

    Page<Student> findByCurrentClassId(Long classId, Pageable pageable);
}
