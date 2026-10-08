package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

@Schema(description = "Geographic point")
public record LocationDto(
        @Schema(description = "Latitude") double latitude,
        @Schema(description = "Longitude") double longitude,
        @Schema(description = "Spatial reference id") Integer srid) {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    public static LocationDto of(double latitude, double longitude) {
        return new LocationDto(latitude, longitude, 4326);
    }

    public Point toPoint() {
        return GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }
}
