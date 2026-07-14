package com.ccb.techfin.dao.sxd;

import com.ccb.techfin.model.sxd.entity.ApplicationAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<ApplicationAttachment, Long> {

    /** 根据 attId 查询单条附件 */
    Optional<ApplicationAttachment> findByAttId(String attId);

    /** 根据 attId 删除附件 */
    void deleteByAttId(String attId);
}
