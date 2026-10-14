package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

@Schema(description = "Geographic point")
public record LocationDto(
        @Schema(description = "Latitude") @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0") double latitude,
        @Schema(description = "Longitude") @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0") double longitude,
        @Schema(description = "Spatial reference id") Integer srid) {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public static LocationDto of(double latitude, double longitude) {
        return new LocationDto(latitude, longitude, 4326);
    }

    public Point toPoint() {
        Point point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
        point.setSRID(srid != null && srid > 0 ? srid : 4326);
        return point;
    }
}
