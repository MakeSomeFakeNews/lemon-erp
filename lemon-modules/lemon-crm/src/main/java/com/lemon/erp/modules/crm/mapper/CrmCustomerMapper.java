package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.crm.entity.CrmCustomer;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

public interface CrmCustomerMapper extends BaseMapper<CrmCustomer> {

    /**
     * 有负责人的客户（我的客户、团队客户，按数据权限过滤）
     *
     * @param ownerId 只看该负责人的客户
     * @param keyword 编码、名称、联系人或电话
     */
    IPage<CrmCustomer> selectOwnedPage(IPage<CrmCustomer> page, @Param("ownerId") Long ownerId, @Param("keyword") String keyword,
                                       @Param("stage") String stage, @Param("industry") String industry,
                                       @Param("dueBefore") LocalDateTime dueBefore);

    /** 公海：没有负责人的启用客户（含还没有 CRM 信息的客户） */
    IPage<CrmCustomer> selectPoolPage(IPage<CrmCustomer> page, @Param("keyword") String keyword,
                                      @Param("stage") String stage, @Param("industry") String industry);

    /** 客户主数据 + 经营信息，customerId 为客户 ID */
    CrmCustomer selectDetail(@Param("customerId") Long customerId);

    /** 客户概况：近一年销售额、订单数、最近下单日期、应收余额、进行中商机、未关闭工单 */
    Map<String, Object> selectSummary(@Param("customerId") Long customerId, @Param("since") LocalDate since);

    /** 领取公海客户：仍在公海时才成功 */
    int claim(@Param("customerId") Long customerId, @Param("ownerId") Long ownerId, @Param("deptId") Long deptId);
}
