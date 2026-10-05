package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.TransportRoute;
import com.sms.entity.TransportStudent;
import com.sms.service.TransportService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/transport")
public class TransportController {

    private final TransportService transportService;

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<PagedResponse<TransportRoute>>> getAllRoutes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TransportRoute> routes = transportService.getAllRoutes(pageable);
        PagedResponse<TransportRoute> response = PagedResponse.<TransportRoute>builder()
                .content(routes.getContent()).page(routes.getNumber()).size(routes.getSize())
                .totalElements(routes.getTotalElements()).totalPages(routes.getTotalPages())
                .first(routes.isFirst()).last(routes.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/routes/all")
    public ResponseEntity<ApiResponse<List<TransportRoute>>> getAllRoutesUnpaged() {
        return ResponseEntity.ok(ApiResponse.success(transportService.getAllRoutes()));
    }

    @GetMapping("/routes/{id}")
    public ResponseEntity<ApiResponse<TransportRoute>> getRoute(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(transportService.getRouteById(id)));
    }

    @PostMapping("/routes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<TransportRoute>> addRoute(@Valid @RequestBody TransportRoute route) {
        return ResponseEntity.ok(ApiResponse.success("Route added", transportService.addRoute(route)));
    }

    @PutMapping("/routes/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<TransportRoute>> updateRoute(@PathVariable Long id, @Valid @RequestBody TransportRoute route) {
        return ResponseEntity.ok(ApiResponse.success("Route updated", transportService.updateRoute(id, route)));
    }

    @DeleteMapping("/routes/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteRoute(@PathVariable Long id) {
        transportService.deleteRoute(id);
        return ResponseEntity.ok(ApiResponse.success("Route deleted", null));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<TransportStudent>> assignStudent(@Valid @RequestBody TransportStudent assignment) {
        return ResponseEntity.ok(ApiResponse.success("Student assigned to route", transportService.assignStudent(assignment)));
    }

    @DeleteMapping("/assign/{assignmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TRANSPORT_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> unassignStudent(@PathVariable Long assignmentId) {
        transportService.unassignStudent(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Student unassigned", null));
    }

    @GetMapping("/routes/{routeId}/students")
    public ResponseEntity<ApiResponse<List<TransportStudent>>> getStudentsByRoute(@PathVariable Long routeId) {
        return ResponseEntity.ok(ApiResponse.success(transportService.getStudentsByRoute(routeId)));
    }

    @GetMapping("/students/{studentId}/route")
    public ResponseEntity<ApiResponse<List<TransportStudent>>> getRouteByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(transportService.getRouteByStudent(studentId)));
    }

    public TransportController(TransportService transportService) {
        this.transportService = transportService;
    }

}
