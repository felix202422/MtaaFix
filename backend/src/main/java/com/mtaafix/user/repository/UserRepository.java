package com.mtaafix.user.repository;

import com.mtaafix.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    long countByRoleAndActiveTrue(User.Role role);

    /** FR-12.1 admin user search: text (email/full name) + role + active filters. */
    @Query("""
            select u from User u
            where (:like is null or lower(u.email) like :like or lower(u.fullName) like :like)
              and (:role is null or u.role = :role)
              and (:active is null or u.active = :active)
            order by u.createdAt desc
            """)
    Page<User> search(@Param("like") String like, @Param("role") User.Role role,
            @Param("active") Boolean active, Pageable pageable);
}
