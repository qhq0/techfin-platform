package com.ccb.techfin.service.sxd.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccb.techfin.common.exception.BusinessException;
import com.ccb.techfin.dao.sxd.CustomerProfileMapper;
import com.ccb.techfin.model.sxd.entity.CustomerProfile;
import com.ccb.techfin.service.sxd.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

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

        List<CustomerProfile> list = customerProfileMapper.selectList(
                new LambdaQueryWrapper<CustomerProfile>()
                        .eq(CustomerProfile::getCstId, cstId)
                        .orderByDesc(CustomerProfile::getDataBsnDt)
                        .last("LIMIT 1"));

        if (list.isEmpty()) {
            throw new BusinessException("CUSTOMER_NOT_FOUND",
                    "客户编号 [" + cstId + "] 不存在");
        }

        String name = list.get(0).getActCntlrNm();
        log.info("Customer controller name query: cstId={}, actCntlrNm={}", cstId, name);
        return name;
    }

    @Override
    public CustomerProfile getCustomerProfile(String cstId) {
        if (!StringUtils.hasText(cstId)) {
            throw new BusinessException("PARAM_MISSING", "客户编号不能为空");
        }

        List<CustomerProfile> list = customerProfileMapper.selectList(
                new LambdaQueryWrapper<CustomerProfile>()
                        .eq(CustomerProfile::getCstId, cstId)
                        .orderByDesc(CustomerProfile::getDataBsnDt)
                        .last("LIMIT 1"));

        if (list.isEmpty()) {
            throw new BusinessException("CUSTOMER_NOT_FOUND",
                    "客户编号 [" + cstId + "] 不存在");
        }

        return list.get(0);
    }
}
