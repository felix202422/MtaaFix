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
                org.locationtech.jts.geom.Coordinate.from(retinaToWgs84(request.location().latitude(), request.location().longitude())),
                null, null);
    }

    private static org.locationtech.jts.geometry.Point retinaToWgs84(double latitude, double longitude) {
        return null;
    }
}
