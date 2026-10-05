package com.sms.aspect;

import tools.jackson.databind.json.JsonMapper;
import com.sms.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;
    private final JsonMapper jsonMapper;

    public AuditAspect(AuditService auditService, JsonMapper jsonMapper) {
        this.auditService = auditService;
        this.jsonMapper = jsonMapper;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Auditable {
        String action() default "ACTION";
        String entity() default "";
    }

    @Pointcut("@annotation(auditable)")
    public void auditableMethods(Auditable auditable) {
    }

    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditAfterReturning(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();
            Long entityId = extractIdFromArgs(args);
            HttpServletRequest request = getCurrentRequest();
            String newValue = result != null ? jsonMapper.writeValueAsString(result) : "";
            auditService.log(
                    auditable.action(),
                    auditable.entity(),
                    entityId,
                    null,
                    newValue,
                    request
            );
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(getClass()).warn("Audit aspect error: {}", e.getMessage());
        }
    }

    @Before("@annotation(auditable)")
    public void auditBefore(JoinPoint joinPoint, Auditable auditable) {
    }

    private Long extractIdFromArgs(Object[] args) {
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof Long) {
                    return (Long) arg;
                }
            }
        }
        return null;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
