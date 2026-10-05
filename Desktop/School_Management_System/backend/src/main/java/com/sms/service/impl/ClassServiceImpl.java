package com.sms.service.impl;

import com.sms.entity.ClassRoom;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.ClassRoomRepository;
import com.sms.service.ClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassServiceImpl implements ClassService {

    private final ClassRoomRepository classRoomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassRoom> getAllClasses() {
        return classRoomRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClassRoom> getClassesPaged(Pageable pageable) {
        return classRoomRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassRoom getClassById(Long id) {
        return classRoomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Class not found with id: " + id));
    }

    @Override
    public ClassRoom createClass(ClassRoom classRoom) {
        return classRoomRepository.save(classRoom);
    }

    @Override
    public ClassRoom updateClass(Long id, ClassRoom classRoom) {
        getClassById(id);
        classRoom.setId(id);
        return classRoomRepository.save(classRoom);
    }

    @Override
    public void deleteClass(Long id) {
        if (!classRoomRepository.existsById(id)) {
            throw new ResourceNotFoundException("Class not found with id: " + id);
        }
        classRoomRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassRoom> getClassesByDepartmentId(Long departmentId) {
        return classRoomRepository.findByDepartmentId(departmentId);
    }

    public ClassServiceImpl(ClassRoomRepository classRoomRepository) {
        this.classRoomRepository = classRoomRepository;
    }

}
