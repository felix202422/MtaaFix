package com.sms.service;

import com.sms.entity.Timetable;

import java.util.List;

public interface TimetableService {
    List<Timetable> getAll();
    Timetable getById(Long id);
    List<Timetable> getByClass(Long classId);
    List<Timetable> getByTeacher(Long teacherId);
    List<Timetable> getByClassAndDay(Long classId, String day);
    List<Timetable> getByTeacherAndDay(Long teacherId, String day);
    Timetable create(Timetable timetable);
    Timetable update(Long id, Timetable timetable);
    void delete(Long id);
}
