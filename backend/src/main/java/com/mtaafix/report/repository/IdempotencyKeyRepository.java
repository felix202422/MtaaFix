package com.mtaafix.report.repository;

import com.mtaafix.report.domain.IdempotencyKey;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, String> {

    /** Live (unexpired) record for this user+endpoint+key, if any. */
    @Query("""
            SELECT k FROM IdempotencyKey k
            WHERE k.user.id = :userId AND k.endpoint = :endpoint
              AND k.idemKey = :key AND k.createdAt > :cutoff
            """)
    IdempotencyKey findActive(@Param("userId") String userId,
            @Param("endpoint") String endpoint,
            @Param("key") String key,
            @Param("cutoff") Instant cutoff);

    @Modifying
    @Query("DELETE FROM IdempotencyKey k WHERE k.createdAt < :cutoff")
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
