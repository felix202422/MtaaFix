package com.mtaafix.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Geographic point")
public record LocationDto(
        @Schema(description = "Latitude") double latitude,
        @Schema(description = "Longitude") double longitude,
        @Schema(description = "Spatial reference id") Integer srid) {

    public static LocationDto of(double latitude, double longitude) {
        return new LocationDto(latitude, longitude, 4326);
    }
}
