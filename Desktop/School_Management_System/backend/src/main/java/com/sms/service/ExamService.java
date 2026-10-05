package com.sms.service;

import com.sms.entity.Examination;
import com.sms.entity.ExamResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ExamService {
    Examination createExamination(Examination exam);
    Examination getExaminationById(Long id);
    List<Examination> getExaminationsByTerm(Long termId);
    Page<Examination> getAllExaminations(Pageable pageable);
    ExamResult addResult(ExamResult result);
    List<ExamResult> getResultsByExam(Long examId);
    List<ExamResult> getResultsByStudent(Long studentId);
    List<ExamResult> getResultsByExamAndStudent(Long examId, Long studentId);
}
