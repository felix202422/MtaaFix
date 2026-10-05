package com.sms.service.impl;

import com.sms.entity.TransportRoute;
import com.sms.entity.TransportStudent;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.TransportRouteRepository;
import com.sms.repository.TransportStudentRepository;
import com.sms.service.TransportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class TransportServiceImpl implements TransportService {

    private final TransportRouteRepository routeRepository;
    private final TransportStudentRepository studentRepository;

    @Override
    public TransportRoute addRoute(TransportRoute route) {
        return routeRepository.save(route);
    }

    @Override
    public TransportRoute updateRoute(Long id, TransportRoute route) {
        getRouteById(id);
        route.setId(id);
        return routeRepository.save(route);
    }

    @Override
    @Transactional(readOnly = true)
    public TransportRoute getRouteById(Long id) {
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transport route not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TransportRoute> getAllRoutes(Pageable pageable) {
        return routeRepository.findAll(pageable);
    }

    @Override
    public void deleteRoute(Long id) {
        if (!routeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transport route not found with id: " + id);
        }
        routeRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransportRoute> getAllRoutes() {
        return routeRepository.findAll();
    }

    @Override
    public TransportStudent assignStudent(TransportStudent assignment) {
        return studentRepository.save(assignment);
    }

    @Override
    public void unassignStudent(Long assignmentId) {
        if (!studentRepository.existsById(assignmentId)) {
            throw new ResourceNotFoundException("Transport assignment not found with id: " + assignmentId);
        }
        studentRepository.deleteById(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransportStudent> getStudentsByRoute(Long routeId) {
        return studentRepository.findByTransportRouteId(routeId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransportStudent> getRouteByStudent(Long studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    public TransportServiceImpl(TransportRouteRepository routeRepository, TransportStudentRepository studentRepository) {
        this.routeRepository = routeRepository;
        this.studentRepository = studentRepository;
    }

}
