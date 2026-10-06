package com.mtaafix.common.utils;

import java.util.UUID;

public final class IdUtils {

    private IdUtils() {
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static String generateReportCode() {
        return "MTF-" + java.time.LocalDate.now(java.time.ZoneId.systemDefault()).format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + "-"
                + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
