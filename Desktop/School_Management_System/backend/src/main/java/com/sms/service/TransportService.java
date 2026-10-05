package com.sms.service;

import com.sms.entity.TransportRoute;
import com.sms.entity.TransportStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransportService {

    TransportRoute addRoute(TransportRoute route);

    TransportRoute updateRoute(Long id, TransportRoute route);

    TransportRoute getRouteById(Long id);

    Page<TransportRoute> getAllRoutes(Pageable pageable);

    void deleteRoute(Long id);

    List<TransportRoute> getAllRoutes();

    TransportStudent assignStudent(TransportStudent assignment);

    void unassignStudent(Long assignmentId);

    List<TransportStudent> getStudentsByRoute(Long routeId);

    List<TransportStudent> getRouteByStudent(Long studentId);
}
