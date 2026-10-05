package com.sms.repository;

import com.sms.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByClassRoomId(Long classRoomId);

    List<Assignment> findByTeacherId(Long teacherId);

    List<Assignment> findBySubjectId(Long subjectId);

    List<Assignment> findByClassRoomIdAndDueDateAfter(Long classRoomId, LocalDateTime date);
}
