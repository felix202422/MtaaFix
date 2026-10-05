package com.sms.repository;

import com.sms.entity.TransportStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransportStudentRepository extends JpaRepository<TransportStudent, Long> {

    List<TransportStudent> findByTransportRouteId(Long routeId);

    List<TransportStudent> findByStudentId(Long studentId);
}
