package com.sms.repository;

import com.sms.entity.HostelRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HostelRoomRepository extends JpaRepository<HostelRoom, Long> {

    List<HostelRoom> findByHostelId(Long hostelId);
}
