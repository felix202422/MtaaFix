package com.sms.repository;

import com.sms.entity.SchoolInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchoolInfoRepository extends JpaRepository<SchoolInfo, Long> {

    Optional<SchoolInfo> findFirstByOrderByIdAsc();
}
