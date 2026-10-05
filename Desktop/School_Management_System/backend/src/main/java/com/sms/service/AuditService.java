package com.sms.service;

import jakarta.servlet.http.HttpServletRequest;

public interface AuditService {
    void log(String action, String entityType, Long entityId, String oldValue, String newValue);
    void log(String action, String entityType, Long entityId, String oldValue, String newValue, HttpServletRequest request);
}
