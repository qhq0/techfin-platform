package com.ccb.techfin.service.sxd;

import com.ccb.techfin.model.sxd.entity.CustomerProfile;

/**
 * 客户信息查询服务
 */
public interface CustomerService {

    /**
     * 根据客户编号查询实控人姓名
     *
     * @param cstId 客户编号
     * @return 实控人姓名
     */
    String getControllerName(String cstId);

    /**
     * 根据客户编号查询客户完整信息
     *
     * @param cstId 客户编号
     * @return 客户信息（最新记录）
     */
    CustomerProfile getCustomerProfile(String cstId);
}
