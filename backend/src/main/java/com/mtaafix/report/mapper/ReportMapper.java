package com.mtaafix.report.mapper;

import com.mtaafix.report.domain.Report;
import com.mtaafix.report.dto.CreateReportRequest;
import com.mtaafix.report.dto.ReportDto;

public final class ReportMapper {

    private ReportMapper() {
    }

    public static ReportDto toDto(Report report) {
        return ReportDto.from(report);
    }

    public static Report toEntity(CreateReportRequest request) {
        return new Report(request.subject(), request.title(), request.description(),
                null, null, null);
    }
}
