package com.mtaafix.report.mapper;

import com.mtaafix.report.domain.Report;
import com.mtaafix.report.domain.ReportCategory;
import com.mtaafix.report.dto.CreateReportRequest;
import com.mtaafix.report.dto.ReportDto;
import com.mtaafix.user.domain.User;

public final class ReportMapper {

    private ReportMapper() {
    }

    public static ReportDto toDto(Report report) {
        return ReportDto.from(report);
    }

    public static Report toEntity(CreateReportRequest request, ReportCategory category, User creator) {
        return new Report(category, request.title(), request.description(),
                request.location() != null ? request.location().toPoint() : null, null, creator);
    }
}
