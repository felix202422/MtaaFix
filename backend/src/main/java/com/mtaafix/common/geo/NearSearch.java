package com.mtaafix.common.geo;

/** Radius search {@code lat,lon,radiusKm} (converted to meters for ST_DWithin). */
public record NearSearch(double latitude, double longitude, double radiusMeters) {

    private static final double MAX_RADIUS_METERS = 500_000;

    public static NearSearch parse(String near) {
        if (near == null || near.isBlank()) {
            return null;
        }
        String[] parts = near.trim().split(",");
        if (parts.length != 3) {
            throw new IllegalArgumentException("near must be 'lat,lon,radiusKm'.");
        }
        double lat;
        double lon;
        double radiusKm;
        try {
            lat = Double.parseDouble(parts[0].trim());
            lon = Double.parseDouble(parts[1].trim());
            radiusKm = Double.parseDouble(parts[2].trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("near must contain only decimal numbers: " + near);
        }
        if (!Double.isFinite(lat) || !Double.isFinite(lon) || !Double.isFinite(radiusKm)) {
            throw new IllegalArgumentException("near values must be finite: " + near);
        }
        if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
            throw new IllegalArgumentException("near center is outside the valid coordinate range.");
        }
        if (radiusKm <= 0 || radiusKm * 1000 > MAX_RADIUS_METERS) {
            throw new IllegalArgumentException("near radiusKm must be > 0 and <= 500.");
        }
        return new NearSearch(lat, lon, radiusKm * 1000.0);
    }
}
