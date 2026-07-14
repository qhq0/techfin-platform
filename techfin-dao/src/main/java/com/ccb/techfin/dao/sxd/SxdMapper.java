package com.ccb.techfin.dao.sxd;

import com.ccb.techfin.model.sxd.entity.ApplicationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SxdMapper extends JpaRepository<ApplicationRecord, String> {

    boolean existsByCreditCode(String creditCode);

    /**
     * 根据 taskId 查询申请记录（taskId 即主键，等同于 findById）。
     */
    Optional<ApplicationRecord> findByTaskId(String taskId);
}
