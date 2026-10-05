package com.sms.service.impl;

import com.sms.entity.Assignment;
import com.sms.entity.AssignmentSubmission;
import com.sms.exception.BadRequestException;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.AssignmentRepository;
import com.sms.repository.AssignmentSubmissionRepository;
import com.sms.service.AssignmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;

    public AssignmentServiceImpl(AssignmentRepository assignmentRepository, AssignmentSubmissionRepository submissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getAllAssignments() {
        return assignmentRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Assignment getAssignmentById(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getByClass(Long classId) {
        return assignmentRepository.findByClassRoomId(classId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getByTeacher(Long teacherId) {
        return assignmentRepository.findByTeacherId(teacherId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Assignment> getUpcomingByClass(Long classId) {
        return assignmentRepository.findByClassRoomIdAndDueDateAfter(classId, LocalDateTime.now());
    }

    @Override
    public Assignment createAssignment(Assignment assignment) {
        if (assignment.getDueDate() == null) {
            throw new BadRequestException("Due date is required");
        }
        return assignmentRepository.save(assignment);
    }

    @Override
    public Assignment updateAssignment(Long id, Assignment assignment) {
        Assignment existing = getAssignmentById(id);
        existing.setTitle(assignment.getTitle());
        existing.setDescription(assignment.getDescription());
        existing.setSubject(assignment.getSubject());
        existing.setClassRoom(assignment.getClassRoom());
        existing.setTeacher(assignment.getTeacher());
        existing.setAcademicYear(assignment.getAcademicYear());
        existing.setDueDate(assignment.getDueDate());
        existing.setMaxMarks(assignment.getMaxMarks());
        existing.setAttachmentUrl(assignment.getAttachmentUrl());
        return assignmentRepository.save(existing);
    }

    @Override
    public void deleteAssignment(Long id) {
        if (!assignmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Assignment not found with id: " + id);
        }
        assignmentRepository.deleteById(id);
    }

    @Override
    public AssignmentSubmission submitAssignment(Long assignmentId, AssignmentSubmission submission) {
        Assignment assignment = getAssignmentById(assignmentId);
        if (submission.getStudent() == null || submission.getStudent().getId() == null) {
            throw new BadRequestException("Student is required");
        }
        if (LocalDateTime.now().isAfter(assignment.getDueDate())) {
            throw new BadRequestException("Assignment deadline has passed");
        }
        submission.setAssignment(assignment);
        submission.setStatus("SUBMITTED");
        submission.setSubmittedAt(LocalDateTime.now());
        return submissionRepository.save(submission);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentSubmission> getSubmissions(Long assignmentId) {
        return submissionRepository.findByAssignmentId(assignmentId);
    }

    @Override
    @Transactional(readOnly = true)
    public AssignmentSubmission getStudentSubmission(Long assignmentId, Long studentId) {
        return submissionRepository.findByAssignmentIdAndStudentId(assignmentId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("No submission found for this student"));
    }

    @Override
    public AssignmentSubmission gradeSubmission(Long submissionId, AssignmentSubmission grading) {
        AssignmentSubmission existing = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found with id: " + submissionId));
        existing.setMarks(grading.getMarks());
        existing.setFeedback(grading.getFeedback());
        existing.setStatus("GRADED");
        return submissionRepository.save(existing);
    }
}
