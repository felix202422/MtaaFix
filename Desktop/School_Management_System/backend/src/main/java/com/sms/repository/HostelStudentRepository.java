package com.sms.repository;

import com.sms.entity.HostelStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HostelStudentRepository extends JpaRepository<HostelStudent, Long> {

    List<HostelStudent> findByHostelRoomId(Long roomId);

    List<HostelStudent> findByStudentId(Long studentId);
}
