package com.mtaafix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mtaafix.common.geo.GeoBounds;
import com.mtaafix.common.geo.GeoJsonPoint;
import com.mtaafix.common.geo.LocationGeneralizer;
import com.mtaafix.common.geo.NearSearch;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

class LocationGeneralizerTest {

    private static final GeometryFactory GF = new GeometryFactory();
    private static final String SECRET = "test-geo-fuzz-secret";

    private LocationGeneralizer generalizer() {
        return new LocationGeneralizer(SECRET, 150, 600);
    }

    private static Point darPoint() {
        return GF.createPoint(new Coordinate(39.20841234, -6.82299876));
    }

    private static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double r = 6_371_000.0;
        double p1 = Math.toRadians(lat1);
        double p2 = Math.toRadians(lat2);
        double dp = Math.toRadians(lat2 - lat1);
        double dl = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2)
                + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    @Test
    void generalizationIsDeterministicPerReport() {
        LocationGeneralizer g = generalizer();
        var a = g.generalize("report-1", darPoint());
        var b = g.generalize("report-1", darPoint());
        assertNotNull(a);
        assertEquals(a.latitude(), b.latitude(), 0.0);
        assertEquals(a.longitude(), b.longitude(), 0.0);
    }

    @Test
    void differentReportsGetDifferentOffsets() {
        LocationGeneralizer g = generalizer();
        var a = g.generalize("report-1", darPoint());
        var b = g.generalize("report-2", darPoint());
        assertNotEquals(a.latitude(), b.latitude());
    }

    @Test
    void offsetStaysWithinConfiguredBounds() {
        LocationGeneralizer g = generalizer();
        for (int i = 0; i < 50; i++) {
            var geo = g.generalize("report-" + i, darPoint());
            double distance = haversineMeters(-6.82299876, 39.20841234,
                    geo.latitude(), geo.longitude());
            assertTrue(distance >= 150.0 - 1.0 && distance <= 600.0 + 1.0,
                    "distance " + distance + " out of bounds for report-" + i);
        }
    }

    @Test
    void differentSecretsProduceDifferentPoints() {
        var a = generalizer().generalize("report-1", darPoint());
        var b = new LocationGeneralizer("another-secret", 150, 600)
                .generalize("report-1", darPoint());
        assertNotEquals(a.latitude(), b.latitude());
    }

    @Test
    void nullLocationOrIdReturnsNull() {
        LocationGeneralizer g = generalizer();
        assertNull(g.generalize("report-1", null));
        assertNull(g.generalize(null, darPoint()));
    }

    @Test
    void geoJsonUsesGeoJsonAxisOrder() {
        var geo = generalizer().generalize("report-1", darPoint());
        GeoJsonPoint point = geo.geoJson();
        assertEquals("Point", point.type());
        assertEquals(2, point.coordinates().size());
        assertEquals(geo.longitude(), point.coordinates().get(0), 0.0,
                "GeoJSON coordinates must be [longitude, latitude]");
        assertEquals(geo.latitude(), point.coordinates().get(1), 0.0);
    }

    @Test
    void invalidBoundsRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class,
                () -> new LocationGeneralizer(SECRET, 600, 150));
        assertThrows(IllegalArgumentException.class,
                () -> new LocationGeneralizer(SECRET, -1, 100));
    }

    // --- GeoBounds parsing ---

    @Test
    void parsesValidBbox() {
        GeoBounds bounds = GeoBounds.parse("39.0,-7.0,40.0,-6.0");
        assertEquals(39.0, bounds.minLongitude());
        assertEquals(-7.0, bounds.minLatitude());
        assertEquals(40.0, bounds.maxLongitude());
        assertEquals(-6.0, bounds.maxLatitude());
        assertNull(GeoBounds.parse(null));
        assertNull(GeoBounds.parse(" "));
    }

    @Test
    void rejectsMalformedBbox() {
        assertThrows(IllegalArgumentException.class, () -> GeoBounds.parse("1,2,3"));
        assertThrows(IllegalArgumentException.class, () -> GeoBounds.parse("a,b,c,d"));
        assertThrows(IllegalArgumentException.class, () -> GeoBounds.parse("40,-7,39,-6"));
        assertThrows(IllegalArgumentException.class, () -> GeoBounds.parse("200,-10,210,-5"));
        assertThrows(IllegalArgumentException.class, () -> GeoBounds.parse("39,95,40,96"));
        assertThrows(IllegalArgumentException.class, () -> GeoBounds.parse("39,-7,39,-6"));
    }

    // --- NearSearch parsing ---

    @Test
    void parsesValidNear() {
        NearSearch near = NearSearch.parse("-6.8,39.2,5");
        assertEquals(-6.8, near.latitude());
        assertEquals(39.2, near.longitude());
        assertEquals(5000.0, near.radiusMeters());
        assertNull(NearSearch.parse(null));
    }

    @Test
    void rejectsMalformedNear() {
        assertThrows(IllegalArgumentException.class, () -> NearSearch.parse("5,30"));
        assertThrows(IllegalArgumentException.class, () -> NearSearch.parse("a,b,c"));
        assertThrows(IllegalArgumentException.class, () -> NearSearch.parse("95,39.2,5"));
        assertThrows(IllegalArgumentException.class, () -> NearSearch.parse("-6.8,200,5"));
        assertThrows(IllegalArgumentException.class, () -> NearSearch.parse("-6.8,39.2,0"));
        assertThrows(IllegalArgumentException.class, () -> NearSearch.parse("-6.8,39.2,600"));
    }
}
