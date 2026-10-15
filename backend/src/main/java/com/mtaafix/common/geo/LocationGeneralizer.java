package com.mtaafix.common.geo;

import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Deterministic public location generalization (FR-11.4). Each report's public
 * coordinates are offset by a stable pseudo-random distance and bearing derived
 * from an HMAC of the report id, so the same report always maps to the same
 * generalized point and markers never jitter between requests.
 */
@Component
public class LocationGeneralizer {

    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    private final byte[] secret;
    private final double minMeters;
    private final double maxMeters;

    public LocationGeneralizer(
            @Value("${geo.fuzz-secret:mtaafix-local-dev-geo-fuzz-secret}") String secret,
            @Value("${geo.fuzz-min-meters:150}") double minMeters,
            @Value("${geo.fuzz-max-meters:600}") double maxMeters) {
        if (minMeters < 0 || maxMeters < minMeters) {
            throw new IllegalArgumentException(
                    "geo fuzz bounds invalid: min=" + minMeters + " max=" + maxMeters);
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.minMeters = minMeters;
        this.maxMeters = maxMeters;
    }

    /** Returns the generalized location, or null when the report has no location. */
    public GeneralizedLocation generalize(String reportId, Point location) {
        if (location == null || reportId == null) {
            return null;
        }
        double exactLat = location.getY();
        double exactLng = location.getX();
        byte[] digest = hmac(reportId);
        double u1 = unsignedFraction(digest, 0);
        double u2 = unsignedFraction(digest, 8);
        double distanceMeters = minMeters + (maxMeters - minMeters) * u1;
        double bearingRadians = 2.0 * Math.PI * u2;
        return destinationPoint(exactLat, exactLng, distanceMeters, bearingRadians);
    }

    public record GeneralizedLocation(double latitude, double longitude) {

        public GeoJsonPoint geoJson() {
            return GeoJsonPoint.of(latitude, longitude);
        }
    }

    private static GeneralizedLocation destinationPoint(double lat, double lng,
            double distanceMeters, double bearingRadians) {
        double phi1 = Math.toRadians(lat);
        double lambda1 = Math.toRadians(lng);
        double delta = distanceMeters / EARTH_RADIUS_METERS;
        double sinPhi2 = Math.sin(phi1) * Math.cos(delta)
                + Math.cos(phi1) * Math.sin(delta) * Math.cos(bearingRadians);
        double phi2 = Math.asin(clamp(sinPhi2, -1.0, 1.0));
        double lambda2 = lambda1 + Math.atan2(
                Math.sin(bearingRadians) * Math.sin(delta) * Math.cos(phi1),
                Math.cos(delta) - Math.sin(phi1) * Math.sin(phi2));
        return new GeneralizedLocation(clamp(Math.toDegrees(phi2), -90.0, 90.0),
                normalizeLongitude(Math.toDegrees(lambda2)));
    }

    private byte[] hmac(String reportId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(reportId.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException ex) {
            throw new IllegalStateException("HMAC-SHA256 unavailable", ex);
        }
    }

    /** Big-endian 8 bytes at offset as a fraction of the unsigned long range [0, 1). */
    private static double unsignedFraction(byte[] bytes, int offset) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (bytes[offset + i] & 0xFFL);
        }
        // Shift to the 53-bit mantissa range, then scale by 2^-53.
        return (value >>> 11) * 0x1.0p-53;
    }

    private static double normalizeLongitude(double degrees) {
        double normalized = ((degrees + 180.0) % 360.0 + 360.0) % 360.0 - 180.0;
        return normalized;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
