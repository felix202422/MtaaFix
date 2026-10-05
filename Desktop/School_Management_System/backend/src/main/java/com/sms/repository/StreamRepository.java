package com.sms.repository;

import com.sms.entity.Stream;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    List<Stream> findByClassRoomId(Long classRoomId);
}
