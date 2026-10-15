package com.mtaafix.common.geo;

/**
 * Geographic filter parameters for list endpoints. Implemented with PostGIS
 * native queries (ST_Intersects / ST_DWithin) — PostgreSQL only; the H2 unit
 * suite validates parsing/400s but does not execute these queries.
 */
public record GeoBounds(double minLongitude, double minLatitude,
        double maxLongitude, double maxLatitude) {

    /**
     * Parses {@code minLon,minLat,maxLon,maxLat} (comma-separated decimals).
     * Throws IllegalArgumentException with a caller-safe message on any problem.
     */
    public static GeoBounds parse(String bbox) {
        if (bbox == null || bbox.isBlank()) {
            return null;
        }
        String[] parts = bbox.trim().split(",");
        if (parts.length != 4) {
            throw new IllegalArgumentException(
                    "bbox must be 'minLon,minLat,maxLon,maxLat' (4 comma-separated numbers).");
        }
        double[] values = new double[4];
        for (int i = 0; i < 4; i++) {
            try {
                values[i] = Double.parseDouble(parts[i].trim());
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(
                        "bbox must contain only decimal numbers: " + bbox);
            }
            if (!Double.isFinite(values[i])) {
                throw new IllegalArgumentException("bbox values must be finite: " + bbox);
            }
        }
        GeoBounds bounds = new GeoBounds(values[0], values[1], values[2], values[3]);
        if (bounds.minLongitude < -180 || bounds.maxLongitude > 180
                || bounds.minLatitude < -90 || bounds.maxLatitude > 90) {
            throw new IllegalArgumentException(
                    "bbox is outside the valid coordinate range (lon ±180, lat ±90).");
        }
        if (bounds.minLongitude >= bounds.maxLongitude
                || bounds.minLatitude >= bounds.maxLatitude) {
            throw new IllegalArgumentException(
                    "bbox requires minLon < maxLon and minLat < maxLat.");
        }
        return bounds;
    }
}
