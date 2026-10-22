package com.mtaafix.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Phase 17 caching (NFR-7, owner-approved scope): Caffeine with a global
 * expireAfterWrite TTL configured in application.yml. Cache names:
 * "categories" (evicted on admin writes), "publicStats" and "adminDashboard"
 * (TTL-only). The user dashboard is deliberately NOT cached — citizens
 * expect their just-created report to appear immediately.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
