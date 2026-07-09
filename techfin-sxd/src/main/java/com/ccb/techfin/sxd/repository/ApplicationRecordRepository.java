package com.ccb.techfin.sxd.repository;

import com.ccb.techfin.sxd.entity.ApplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ApplicationRecordRepository extends JpaRepository<ApplicationRecord, Long> {

    boolean existsByCreditCode(String creditCode);

    Optional<ApplicationRecord> findByTaskId(String taskId);
}
