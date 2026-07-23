package com.ccb.techfin.service.sxd.impl;

import com.ccb.techfin.common.exception.BusinessException;
import com.ccb.techfin.dao.sxd.CustomerProfileMapper;
import com.ccb.techfin.dao.sxd.SxdMapper;
import com.ccb.techfin.model.sxd.entity.SxdRecord;
import com.ccb.techfin.model.sxd.entity.CustomerProfile;
import com.ccb.techfin.service.sxd.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 客户信息服务实现。
 *
 * @author qiuhaoquan
 * @since 2026-07-23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerProfileMapper customerProfileMapper;
    private final SxdMapper sxdMapper;

    @Override
    public String getControllerName(String cstId) {
        if (!StringUtils.hasText(cstId)) {
            throw new BusinessException("PARAM_MISSING", "客户编号不能为空");
        }

        CustomerProfile profile = customerProfileMapper.selectById(cstId);

        if (profile == null) {
            throw new BusinessException("CUSTOMER_NOT_FOUND",
                    "客户编号 [" + cstId + "] 不存在");
        }

        String name = profile.getActCntlrNm();
        log.info("Customer controller name query: cstId={}, actCntlrNm={}", cstId, name);
        return name;
    }

    @Override
    public CustomerProfile getCustomerProfile(String cstId) {
        if (!StringUtils.hasText(cstId)) {
            throw new BusinessException("PARAM_MISSING", "客户编号不能为空");
        }

        CustomerProfile profile = customerProfileMapper.selectById(cstId);

        if (profile == null) {
            throw new BusinessException("CUSTOMER_NOT_FOUND",
                    "客户编号 [" + cstId + "] 不存在");
        }

        sanitizeProfile(profile);

        return profile;
    }

    /**
     * 清洗 sxd_profile 中特定字段的占位值：
     * tech_flow 的 "-" → ""，其余 5 个字段的 "-99" → ""。
     */
    private void sanitizeProfile(CustomerProfile profile) {
        if ("-".equals(profile.getTechFlow())) {
            profile.setTechFlow("");
        }
        if ("-99".equals(profile.getKcScore())) {
            profile.setKcScore("");
        }
        if ("-99".equals(profile.getEntpPtntNum())) {
            profile.setEntpPtntNum("");
        }
        if ("-99".equals(profile.getEntpPrctNewTpPtntNum())) {
            profile.setEntpPrctNewTpPtntNum("");
        }
        if ("-99".equals(profile.getEntpIvtPtntNum())) {
            profile.setEntpIvtPtntNum("");
        }
        if ("-99".equals(profile.getClst5YrInnRsWcoprNum())) {
            profile.setClst5YrInnRsWcoprNum("");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean getCustOwnership(String taskId, String cstId, String userId) {
        if (!StringUtils.hasText(taskId)) {
            throw new BusinessException("PARAM_MISSING", "任务 ID 不能为空");
        }
        if (!StringUtils.hasText(cstId)) {
            throw new BusinessException("PARAM_MISSING", "客户编号不能为空");
        }

        // 查询 sxd_profile 获取管户信息
        CustomerProfile profile = customerProfileMapper.selectById(cstId);
        if (profile == null) {
            throw new BusinessException("CUSTOMER_NOT_FOUND",
                    "客户编号 [" + cstId + "] 不存在");
        }

        log.info("Cust ownership query: taskId={}, cstId={}, cstMngaccCstmgrId={}, cstMngaccInstSuprInsid={}, userId={}",
                taskId, cstId,
                profile.getCstMngaccCstmgrId(), profile.getCstMngaccInstSuprInsid(), userId);

        // TODO: 管户权判断逻辑待定，结合 userId、cstMngaccCstmgrId、cstMngaccInstSuprInsid 判断
        String hasOwnership = "1";

        // 写入 sxd_record
        SxdRecord record = sxdMapper.selectById(taskId);
        if (record != null) {
            record.setHasOwnership(hasOwnership);
            sxdMapper.updateById(record);
        } else {
            log.warn("Task not found for ownership update: taskId={}", taskId);
        }

        return "1".equals(hasOwnership);
    }
}
