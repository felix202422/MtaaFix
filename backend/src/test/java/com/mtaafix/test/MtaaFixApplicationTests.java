package com.mtaafix.test;

import com.mtaafix.MtaaFixApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MtaaFixApplication.class)
@ActiveProfiles("test")
class MtaaFixApplicationTests {

    @Test
    void contextLoads() {
        // The Spring context must load with Java 25, Spring Boot 4.1.1,
        // PostgreSQL + PostGIS dependencies, and Flyway migrations enabled.
    }
}
