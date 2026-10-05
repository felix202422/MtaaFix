package com.sms.service.impl;

import com.sms.entity.Timetable;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.TimetableRepository;
import com.sms.service.TimetableService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TimetableServiceImpl implements TimetableService {

    private final TimetableRepository timetableRepository;

    public TimetableServiceImpl(TimetableRepository timetableRepository) {
        this.timetableRepository = timetableRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Timetable> getAll() {
        return timetableRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Timetable getById(Long id) {
        return timetableRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Timetable entry not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Timetable> getByClass(Long classId) {
        return timetableRepository.findByClassRoomId(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Timetable> getByTeacher(Long teacherId) {
        return timetableRepository.findByTeacherId(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Timetable> getByClassAndDay(Long classId, String day) {
        return timetableRepository.findByClassRoomIdAndDayOfWeek(classId, day);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Timetable> getByTeacherAndDay(Long teacherId, String day) {
        return timetableRepository.findByTeacherIdAndDayOfWeek(teacherId, day);
    }

    @Override
    public Timetable create(Timetable timetable) {
        return timetableRepository.save(timetable);
    }

    @Override
    public Timetable update(Long id, Timetable timetable) {
        Timetable existing = getById(id);
        existing.setClassRoom(timetable.getClassRoom());
        existing.setSubject(timetable.getSubject());
        existing.setTeacher(timetable.getTeacher());
        existing.setDayOfWeek(timetable.getDayOfWeek());
        existing.setStartTime(timetable.getStartTime());
        existing.setEndTime(timetable.getEndTime());
        existing.setRoom(timetable.getRoom());
        existing.setAcademicYear(timetable.getAcademicYear());
        existing.setTerm(timetable.getTerm());
        return timetableRepository.save(existing);
    }

    @Override
    public void delete(Long id) {
        if (!timetableRepository.existsById(id)) {
            throw new ResourceNotFoundException("Timetable entry not found with id: " + id);
        }
        timetableRepository.deleteById(id);
    }
}
