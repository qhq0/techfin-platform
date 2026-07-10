package com.ccb.techfin.dao.sxd;

import com.ccb.techfin.model.sxd.entity.ApplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SxdMapper extends JpaRepository<ApplicationRecord, Long> {

    boolean existsByCreditCode(String creditCode);

    Optional<ApplicationRecord> findByTaskId(String taskId);
}
