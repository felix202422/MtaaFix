package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.dto.response.PagedResponse;
import com.sms.entity.Hostel;
import com.sms.entity.HostelRoom;
import com.sms.entity.HostelStudent;
import com.sms.service.HostelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/hostel")
public class HostelController {

    private final HostelService hostelService;

    @GetMapping("/hostels")
    public ResponseEntity<ApiResponse<PagedResponse<Hostel>>> getAllHostels(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Hostel> hostels = hostelService.getAllHostels(pageable);
        PagedResponse<Hostel> response = PagedResponse.<Hostel>builder()
                .content(hostels.getContent()).page(hostels.getNumber()).size(hostels.getSize())
                .totalElements(hostels.getTotalElements()).totalPages(hostels.getTotalPages())
                .first(hostels.isFirst()).last(hostels.isLast()).build();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/hostels/all")
    public ResponseEntity<ApiResponse<List<Hostel>>> getAllHostelsUnpaged() {
        return ResponseEntity.ok(ApiResponse.success(hostelService.getAllHostels()));
    }

    @GetMapping("/hostels/{id}")
    public ResponseEntity<ApiResponse<Hostel>> getHostel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hostelService.getHostelById(id)));
    }

    @PostMapping("/hostels")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'HOSTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Hostel>> addHostel(@Valid @RequestBody Hostel hostel) {
        return ResponseEntity.ok(ApiResponse.success("Hostel added", hostelService.addHostel(hostel)));
    }

    @PutMapping("/hostels/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'HOSTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Hostel>> updateHostel(@PathVariable Long id, @Valid @RequestBody Hostel hostel) {
        return ResponseEntity.ok(ApiResponse.success("Hostel updated", hostelService.updateHostel(id, hostel)));
    }

    @DeleteMapping("/hostels/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HOSTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteHostel(@PathVariable Long id) {
        hostelService.deleteHostel(id);
        return ResponseEntity.ok(ApiResponse.success("Hostel deleted", null));
    }

    @GetMapping("/rooms/hostel/{hostelId}")
    public ResponseEntity<ApiResponse<List<HostelRoom>>> getRoomsByHostel(@PathVariable Long hostelId) {
        return ResponseEntity.ok(ApiResponse.success(hostelService.getRoomsByHostel(hostelId)));
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<HostelRoom>> getRoom(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hostelService.getRoomById(id)));
    }

    @PostMapping("/rooms")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'HOSTEL_MANAGER')")
    public ResponseEntity<ApiResponse<HostelRoom>> addRoom(@Valid @RequestBody HostelRoom room) {
        return ResponseEntity.ok(ApiResponse.success("Room added", hostelService.addRoom(room)));
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'HOSTEL_MANAGER')")
    public ResponseEntity<ApiResponse<HostelRoom>> updateRoom(@PathVariable Long id, @Valid @RequestBody HostelRoom room) {
        return ResponseEntity.ok(ApiResponse.success("Room updated", hostelService.updateRoom(id, room)));
    }

    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'HOSTEL_MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        hostelService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.success("Room deleted", null));
    }

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'HOSTEL_MANAGER')")
    public ResponseEntity<ApiResponse<HostelStudent>> assignStudent(@Valid @RequestBody HostelStudent assignment) {
        return ResponseEntity.ok(ApiResponse.success("Student assigned to room", hostelService.assignStudent(assignment)));
    }

    @PostMapping("/checkout/{assignmentId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SCHOOL_ADMIN', 'HOSTEL_MANAGER')")
    public ResponseEntity<ApiResponse<HostelStudent>> checkOutStudent(@PathVariable Long assignmentId) {
        hostelService.checkOutStudent(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Student checked out", null));
    }

    @GetMapping("/rooms/{roomId}/students")
    public ResponseEntity<ApiResponse<List<HostelStudent>>> getStudentsByRoom(@PathVariable Long roomId) {
        return ResponseEntity.ok(ApiResponse.success(hostelService.getStudentsByRoom(roomId)));
    }

    @GetMapping("/students/{studentId}/room")
    public ResponseEntity<ApiResponse<List<HostelStudent>>> getRoomByStudent(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(hostelService.getRoomByStudent(studentId)));
    }

    public HostelController(HostelService hostelService) {
        this.hostelService = hostelService;
    }

}
