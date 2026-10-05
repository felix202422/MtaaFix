package com.sms.service;

import java.util.Map;

public interface DashboardService {
    Map<String, Object> getAdminDashboard();
    Map<String, Object> getTeacherDashboard(Long teacherId);
    Map<String, Object> getStudentDashboard(Long studentId);
    Map<String, Object> getParentDashboard(Long parentId);
}
