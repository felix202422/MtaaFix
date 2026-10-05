package com.sms.service;

import com.sms.entity.Assignment;
import com.sms.entity.AssignmentSubmission;

import java.util.List;

public interface AssignmentService {
    List<Assignment> getAllAssignments();
    Assignment getAssignmentById(Long id);
    List<Assignment> getByClass(Long classId);
    List<Assignment> getByTeacher(Long teacherId);
    List<Assignment> getUpcomingByClass(Long classId);
    Assignment createAssignment(Assignment assignment);
    Assignment updateAssignment(Long id, Assignment assignment);
    void deleteAssignment(Long id);

    AssignmentSubmission submitAssignment(Long assignmentId, AssignmentSubmission submission);
    List<AssignmentSubmission> getSubmissions(Long assignmentId);
    AssignmentSubmission getStudentSubmission(Long assignmentId, Long studentId);
    AssignmentSubmission gradeSubmission(Long submissionId, AssignmentSubmission grading);
}
