package com.mtaafix.common.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtils {

    public static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ofPattern(
            "yyyy-MM-dd'T'HH:mm:ss'Z'");
    public static final DateTimeFormatter ISO_LOCAL_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateTimeUtils() {
    }

    public static Instant toInstant(String isoString) {
        if (isoString == null) {
            return null;
        }
        return Instant.parse(isoString);
    }

    public static String toIsoString(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(ZoneId.systemDefault()).toLocalDate().format(ISO_LOCAL_DATE);
    }

    public static String toIsoStringLocalDate(LocalDate localDate) {
        if (localDate == null) {
            return null;
        }
        return localDate.format(ISO_LOCAL_DATE);
    }

    public static String nowIso() {
        return Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().format(ISO_LOCAL_DATE);
    }
}
