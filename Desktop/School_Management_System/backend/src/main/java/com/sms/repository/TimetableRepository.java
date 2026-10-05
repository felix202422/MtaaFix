package com.sms.repository;

import com.sms.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {

    List<Timetable> findByClassRoomId(Long classRoomId);

    List<Timetable> findByTeacherId(Long teacherId);

    List<Timetable> findByClassRoomIdAndDayOfWeek(Long classRoomId, String dayOfWeek);

    List<Timetable> findByTeacherIdAndDayOfWeek(Long teacherId, String dayOfWeek);
}
