package com.lemon.erp.modules.crm.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 离职交接：负责人改为交接人，负责人部门（数据权限）取交接人的部门；没有交接人时客户退回公海
 */
public interface CrmHandoverMapper {

    int handoverCustomers(@Param("from") Long from, @Param("to") Long to);

    int handoverLeads(@Param("from") Long from, @Param("to") Long to);

    int handoverOpportunities(@Param("from") Long from, @Param("to") Long to);

    int handoverTickets(@Param("from") Long from, @Param("to") Long to);
}
