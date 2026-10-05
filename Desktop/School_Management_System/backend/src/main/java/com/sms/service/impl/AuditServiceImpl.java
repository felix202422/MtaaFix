package com.sms.service.impl;

import com.sms.entity.AuditLog;
import com.sms.entity.User;
import com.sms.repository.AuditLogRepository;
import com.sms.repository.UserRepository;
import com.sms.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId, String oldValue, String newValue) {
        log(action, entityType, entityId, oldValue, newValue, null);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(String action, String entityType, Long entityId, String oldValue, String newValue, HttpServletRequest request) {
        try {
            User currentUser = null;
            var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof com.sms.security.CustomUserDetails) {
                currentUser = ((com.sms.security.CustomUserDetails) auth.getPrincipal()).getUser();
            }

            AuditLog auditLog = new AuditLog();
            auditLog.setUser(currentUser);
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setOldValue(oldValue);
            auditLog.setNewValue(newValue);
            if (request != null) {
                auditLog.setIpAddress(request.getRemoteAddr());
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            // Audit logging should never break the main flow
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Audit log failed: {}", e.getMessage());
        }
    }
}
