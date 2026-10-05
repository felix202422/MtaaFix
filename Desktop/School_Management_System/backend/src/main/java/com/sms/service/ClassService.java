package com.sms.service;

import com.sms.entity.ClassRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ClassService {
    List<ClassRoom> getAllClasses();
    Page<ClassRoom> getClassesPaged(Pageable pageable);
    ClassRoom getClassById(Long id);
    ClassRoom createClass(ClassRoom classRoom);
    ClassRoom updateClass(Long id, ClassRoom classRoom);
    void deleteClass(Long id);
    List<ClassRoom> getClassesByDepartmentId(Long departmentId);
}
