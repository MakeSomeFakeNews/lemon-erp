package com.lemon.erp.modules.crm.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.record.RecordType;
import com.lemon.erp.framework.record.RecordTypeProvider;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.crm.entity.CrmCustomer;
import com.lemon.erp.modules.crm.mapper.CrmCustomerMapper;
import com.lemon.erp.modules.crm.mapper.CrmOpportunityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 客户关系可挂附件、记录变更历史的记录：客户、商机（方案、报价单）
 */
@Component
@RequiredArgsConstructor
public class CrmRecordTypes implements RecordTypeProvider {

    private static final DataPermissionRule CUSTOMER_RULE = new DataPermissionRule("crm_customer", "dept_id", "owner_id");

    private final CrmCustomerMapper customerMapper;
    private final CrmOpportunityMapper opportunityMapper;

    /** 业务 ID 为客户主数据 ID：公海客户（没有负责人）所有人可见，有负责人的按数据权限过滤 */
    private Object customer(long customerId) {
        CrmCustomer c = customerMapper.selectOne(Wrappers.<CrmCustomer>lambdaQuery().eq(CrmCustomer::getCustomerId, customerId));
        if (c == null) {
            return customerMapper.selectDetail(customerId);
        }
        if (c.getOwnerId() == null || SecurityUtils.getDataScope().all()) {
            return c;
        }
        Long id = c.getId();
        boolean visible = DataPermissionContext.execute(CUSTOMER_RULE,
                () -> customerMapper.selectCount(Wrappers.<CrmCustomer>lambdaQuery().eq(CrmCustomer::getId, id)) > 0);
        return visible ? c : null;
    }

    @Override
    public List<RecordType> recordTypes() {
        return List.of(
                // 业务 ID 为客户主数据 ID（scm_customer），按客户经营信息的负责人、部门过滤
                new RecordType("crm_customer", "客户", "crm:customer:list", "crm:customer:edit", null, this::customer), // i18n-ignore 记录类型名称，输出处翻译
                new RecordType("crm_opp", "商机", "crm:opp:list", "crm:opp:edit", // i18n-ignore 记录类型名称，输出处翻译
                        new DataPermissionRule("crm_opportunity", "dept_id", "owner_id"), opportunityMapper::selectById));
    }
}
