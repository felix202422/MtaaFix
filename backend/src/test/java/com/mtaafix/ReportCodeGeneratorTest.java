package com.mtaafix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mtaafix.report.service.ReportCodeGenerator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = MtaaFixApplication.class)
@ActiveProfiles("test")
class ReportCodeGeneratorTest {

    @Autowired
    private ReportCodeGenerator generator;

    @Test
    void concurrentGenerationProducesUniqueWellFormedCodes() throws Exception {
        int threads = 4;
        int perThread = 15;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<List<String>>> tasks = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                tasks.add(() -> {
                    List<String> codes = new ArrayList<>();
                    for (int i = 0; i < perThread; i++) {
                        codes.add(generator.next());
                    }
                    return codes;
                });
            }
            List<Future<List<String>>> futures = pool.invokeAll(tasks);
            List<String> all = new ArrayList<>();
            for (Future<List<String>> future : futures) {
                all.addAll(future.get(30, TimeUnit.SECONDS));
            }
            assertEquals(threads * perThread, all.size());
            Set<String> distinct = new HashSet<>(all);
            assertEquals(all.size(), distinct.size(), "Generated report codes must be unique");
            for (String code : all) {
                assertTrue(code.matches("MTF-\\d{4}-\\d{6}"),
                        "Code must match MTF-yyyy-NNNNNN but was " + code);
            }
        } finally {
            pool.shutdownNow();
        }
    }
}
