package com.sms.service.impl;

import com.sms.entity.Hostel;
import com.sms.entity.HostelRoom;
import com.sms.entity.HostelStudent;
import com.sms.exception.BadRequestException;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.HostelRepository;
import com.sms.repository.HostelRoomRepository;
import com.sms.repository.HostelStudentRepository;
import com.sms.service.HostelService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class HostelServiceImpl implements HostelService {

    private final HostelRepository hostelRepository;
    private final HostelRoomRepository roomRepository;
    private final HostelStudentRepository studentRepository;

    @Override
    public Hostel addHostel(Hostel hostel) {
        return hostelRepository.save(hostel);
    }

    @Override
    public Hostel updateHostel(Long id, Hostel hostel) {
        getHostelById(id);
        hostel.setId(id);
        return hostelRepository.save(hostel);
    }

    @Override
    @Transactional(readOnly = true)
    public Hostel getHostelById(Long id) {
        return hostelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Hostel> getAllHostels(Pageable pageable) {
        return hostelRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hostel> getAllHostels() {
        return hostelRepository.findAll();
    }

    @Override
    public void deleteHostel(Long id) {
        if (!hostelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hostel not found with id: " + id);
        }
        hostelRepository.deleteById(id);
    }

    @Override
    public HostelRoom addRoom(HostelRoom room) {
        return roomRepository.save(room);
    }

    @Override
    public HostelRoom updateRoom(Long id, HostelRoom room) {
        getRoomById(id);
        room.setId(id);
        return roomRepository.save(room);
    }

    @Override
    @Transactional(readOnly = true)
    public HostelRoom getRoomById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel room not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<HostelRoom> getRoomsByHostel(Long hostelId) {
        return roomRepository.findByHostelId(hostelId);
    }

    @Override
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Hostel room not found with id: " + id);
        }
        roomRepository.deleteById(id);
    }

    @Override
    public HostelStudent assignStudent(HostelStudent assignment) {
        HostelRoom room = assignment.getHostelRoom();
        if (room.getOccupied() >= room.getCapacity()) {
            throw new BadRequestException("Room is already full");
        }
        room.setOccupied(room.getOccupied() + 1);
        roomRepository.save(room);
        if (assignment.getCheckInDate() == null) {
            assignment.setCheckInDate(LocalDate.now());
        }
        if (assignment.getStatus() == null) {
            assignment.setStatus("ACTIVE");
        }
        return studentRepository.save(assignment);
    }

    @Override
    public void checkOutStudent(Long assignmentId) {
        HostelStudent assignment = studentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Hostel assignment not found with id: " + assignmentId));
        if ("ACTIVE".equals(assignment.getStatus())) {
            HostelRoom room = assignment.getHostelRoom();
            room.setOccupied(Math.max(0, room.getOccupied() - 1));
            roomRepository.save(room);
        }
        assignment.setCheckOutDate(LocalDate.now());
        assignment.setStatus("CHECKED_OUT");
        studentRepository.save(assignment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HostelStudent> getStudentsByRoom(Long roomId) {
        return studentRepository.findByHostelRoomId(roomId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HostelStudent> getRoomByStudent(Long studentId) {
        return studentRepository.findByStudentId(studentId);
    }

    public HostelServiceImpl(HostelRepository hostelRepository, HostelRoomRepository roomRepository, HostelStudentRepository studentRepository) {
        this.hostelRepository = hostelRepository;
        this.roomRepository = roomRepository;
        this.studentRepository = studentRepository;
    }

}
