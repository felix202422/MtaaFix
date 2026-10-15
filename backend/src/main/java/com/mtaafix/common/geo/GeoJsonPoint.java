package com.mtaafix.common.geo;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "GeoJSON Point (FR-11.5); coordinates are [longitude, latitude]")
public record GeoJsonPoint(String type, List<Double> coordinates) {

    public static GeoJsonPoint of(double latitude, double longitude) {
        return new GeoJsonPoint("Point", List.of(longitude, latitude));
    }
}
