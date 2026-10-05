package com.sms.service;

import com.sms.entity.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface TeacherService {
    Page<Teacher> getAllTeachers(Pageable pageable);
    Teacher getTeacherById(Long id);
    Teacher createTeacher(Teacher teacher);
    Teacher updateTeacher(Long id, Teacher teacher);
    void deleteTeacher(Long id);
    List<Teacher> getTeachersByDepartmentId(Long departmentId);
    long getTeacherCount();
}
