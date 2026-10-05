package com.sms.service.impl;

import com.sms.entity.Examination;
import com.sms.entity.ExamResult;
import com.sms.exception.ResourceNotFoundException;
import com.sms.repository.ExaminationRepository;
import com.sms.repository.ExamResultRepository;
import com.sms.service.ExamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ExamServiceImpl implements ExamService {

    private final ExaminationRepository examinationRepository;
    private final ExamResultRepository examResultRepository;

    @Override
    public Examination createExamination(Examination exam) {
        return examinationRepository.save(exam);
    }

    @Override
    @Transactional(readOnly = true)
    public Examination getExaminationById(Long id) {
        return examinationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Examination not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Examination> getExaminationsByTerm(Long termId) {
        return examinationRepository.findByTermId(termId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Examination> getAllExaminations(Pageable pageable) {
        return examinationRepository.findAll(pageable);
    }

    @Override
    public ExamResult addResult(ExamResult result) {
        return examResultRepository.save(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamResult> getResultsByExam(Long examId) {
        return examResultRepository.findByExaminationId(examId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamResult> getResultsByStudent(Long studentId) {
        return examResultRepository.findByStudentId(studentId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamResult> getResultsByExamAndStudent(Long examId, Long studentId) {
        return examResultRepository.findByExaminationIdAndStudentId(examId, studentId);
    }

    public ExamServiceImpl(ExaminationRepository examinationRepository, ExamResultRepository examResultRepository) {
        this.examinationRepository = examinationRepository;
        this.examResultRepository = examResultRepository;
    }

}
