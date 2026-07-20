package com.ccb.techfin.service.sxd.impl;

import com.ccb.techfin.common.exception.BusinessException;
import com.ccb.techfin.dao.sxd.CustomerProfileMapper;
import com.ccb.techfin.model.sxd.entity.CustomerProfile;
import com.ccb.techfin.service.sxd.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceImpl implements CustomerService {

    private final CustomerProfileMapper customerProfileMapper;

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

        return profile;
    }

    @Override
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
        return true;
    }
}
