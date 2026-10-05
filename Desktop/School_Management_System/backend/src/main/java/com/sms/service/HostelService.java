package com.sms.service;

import com.sms.entity.Hostel;
import com.sms.entity.HostelRoom;
import com.sms.entity.HostelStudent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HostelService {

    Hostel addHostel(Hostel hostel);

    Hostel updateHostel(Long id, Hostel hostel);

    Hostel getHostelById(Long id);

    Page<Hostel> getAllHostels(Pageable pageable);

    List<Hostel> getAllHostels();

    void deleteHostel(Long id);

    HostelRoom addRoom(HostelRoom room);

    HostelRoom updateRoom(Long id, HostelRoom room);

    HostelRoom getRoomById(Long id);

    List<HostelRoom> getRoomsByHostel(Long hostelId);

    void deleteRoom(Long id);

    HostelStudent assignStudent(HostelStudent assignment);

    void checkOutStudent(Long assignmentId);

    List<HostelStudent> getStudentsByRoom(Long roomId);

    List<HostelStudent> getRoomByStudent(Long studentId);
}
