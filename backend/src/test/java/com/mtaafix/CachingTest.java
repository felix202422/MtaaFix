package com.mtaafix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mtaafix.category.dto.CategoryDto;
import com.mtaafix.category.service.CategoryService;
import com.mtaafix.report.repository.ReportCategoryRepository;
import com.mtaafix.report.repository.ReportRepository;
import com.mtaafix.stats.service.PublicStatsService;
import com.mtaafix.user.domain.User;
import com.mtaafix.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Phase 17 (NFR-7): proves the Caffeine caches actually short-circuit
 * repository access, that category mutations evict, and that the test profile
 * default (cache off) is overridden here so the assertions are meaningful.
 */
@SpringBootTest(classes = MtaaFixApplication.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cache.type=caffeine",
        "spring.cache.caffeine.spec=expireAfterWrite=60s"
})
@Import(CachingTest.CacheManagerConfig.class)
class CachingTest {

    @TestConfiguration
    static class CacheManagerConfig {
        // Marker config class — @EnableCaching comes from the production
        // CacheConfig; this exists so the test slice can reference a nested
        // class for readability without pulling extra beans.
    }

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private PublicStatsService publicStatsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @MockitoSpyBean
    private ReportCategoryRepository categoryRepository;

    @MockitoSpyBean
    private ReportRepository reportRepository;

    @BeforeEach
    void clearCaches() {
        // The Caffeine cache outlives test methods within the shared context;
        // reset so each test starts cold and verify() counts stay isolated.
        var categories = cacheManager.getCache("categories");
        if (categories != null) {
            categories.clear();
        }
        var publicStats = cacheManager.getCache("publicStats");
        if (publicStats != null) {
            publicStats.clear();
        }
    }

    @Test
    void categoryListIsCachedAndMutationsEvict() {
        // First call hits the repository.
        categoryService.listAll();
        // Second call is served from the cache — no extra repository hit.
        categoryService.listAll();
        verify(categoryRepository, times(1)).findAll(any(Sort.class));

        // Admin mutation evicts the cache (categories + all keys).
        User admin = new User();
        admin.setEmail("cache-admin-" + System.nanoTime() + "@example.com");
        admin.setPasswordHash("hash");
        admin.setFullName("Cache Admin");
        admin.setRole(User.Role.ADMINISTRATOR);
        admin = userRepository.save(admin);

        String slug = "cache-" + System.nanoTime();
        CategoryDto created = categoryService.create(
                new CategoryDto(null, "Cache category", slug,
                        com.mtaafix.report.domain.ReportCategory.Type.PRIMARY,
                        null, true, null), admin);

        // Post-eviction call hits the repository again.
        categoryService.listAll();
        verify(categoryRepository, times(2)).findAll(any(Sort.class));

        // And the created category is visible in the fresh result.
        assertFalse(categoryService.listAll().stream()
                .noneMatch(c -> slug.equals(c.slug())));
        assertEquals(created.slug(), slug);
    }

    @Test
    void publicStatsIsCached() {
        publicStatsService.getStats();
        publicStatsService.getStats();
        verify(reportRepository, times(1)).count();
    }

    @Test
    void listAllAndListWithDifferentKeysAreCachedSeparately() {
        categoryService.listAll();
        categoryService.list(true);
        // listAll uses key 'active'; list(true) uses key Boolean.TRUE — both
        // map to the same query but are distinct cache keys, so two repository
        // hits are expected on a cold cache.
        verify(categoryRepository, times(2)).findAll(any(Sort.class));
        categoryService.list(true);
        verify(categoryRepository, times(2)).findAll(any(Sort.class));
    }
}
