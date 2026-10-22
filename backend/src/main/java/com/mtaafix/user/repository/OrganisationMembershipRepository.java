package com.mtaafix.user.repository;

import com.mtaafix.user.domain.OrganisationMembership;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrganisationMembershipRepository extends JpaRepository<OrganisationMembership, String> {

    Optional<OrganisationMembership> findByUserId(String userId);

    Optional<OrganisationMembership> findByUserIdAndOrganisationId(String userId, String organisationId);

    List<OrganisationMembership> findByOrganisationIdOrderByJoinedAtAsc(String organisationId);

    /** Phase 17: paginated member listing (NFR-7). */
    org.springframework.data.domain.Page<OrganisationMembership> findByOrganisationId(
            String organisationId, org.springframework.data.domain.Pageable pageable);

    long countByOrganisationId(String organisationId);

    @Modifying
    @Query("delete from OrganisationMembership m where m.userId = :userId")
    void deleteByUserId(@Param("userId") String userId);
}
