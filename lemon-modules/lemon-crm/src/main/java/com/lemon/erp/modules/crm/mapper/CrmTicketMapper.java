package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.crm.entity.CrmTicket;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface CrmTicketMapper extends BaseMapper<CrmTicket> {

    /**
     * @param keyword 工单号、标题或客户名称
     * @param overdue true 只看超时未解决的
     */
    IPage<CrmTicket> selectPageList(IPage<CrmTicket> page, @Param("keyword") String keyword, @Param("type") String type,
                                    @Param("priority") String priority, @Param("status") String status,
                                    @Param("customerId") Long customerId, @Param("assigneeId") Long assigneeId,
                                    @Param("overdue") Boolean overdue);

    CrmTicket selectDetail(@Param("id") Long id);

    /** 序列号最近一次销售出库（物料、出库单、日期、客户） */
    List<Map<String, Object>> selectSerialSales(@Param("serialNo") String serialNo);
}
