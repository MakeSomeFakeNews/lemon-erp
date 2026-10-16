package com.lemon.erp.modules.crm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.crm.entity.CrmTicketPart;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CrmTicketPartMapper extends BaseMapper<CrmTicketPart> {

    List<CrmTicketPart> selectByTicket(@Param("ticketId") Long ticketId);
}
