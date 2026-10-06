package com.mtaafix.report.repository;

import com.mtaafix.report.domain.Organisation;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepository extends JpaRepository<Organisation, String> {
    Optional<Organisation> findBySlug(String slug);
}
