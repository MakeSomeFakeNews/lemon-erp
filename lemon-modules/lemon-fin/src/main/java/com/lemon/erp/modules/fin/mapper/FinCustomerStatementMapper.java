package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinCustomerStatement;
import com.lemon.erp.modules.fin.entity.FinCustomerStatementLine;
import com.lemon.erp.modules.fin.model.StatementStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinCustomerStatementMapper extends BaseMapper<FinCustomerStatement> {

    /** @param customerId 经销商门户只看本客户；门户不显示草稿 */
    IPage<FinCustomerStatement> selectPageList(IPage<FinCustomerStatement> page, @Param("customerId") Long customerId,
                                               @Param("status") StatementStatus status, @Param("keyword") String keyword,
                                               @Param("portal") boolean portal);

    FinCustomerStatement selectDetail(@Param("id") Long id);

    /** @param page 为 null 时返回全部（打印、导出） */
    List<FinCustomerStatementLine> selectLines(IPage<FinCustomerStatementLine> page, @Param("statementId") Long statementId);

    int insertLines(@Param("lines") List<FinCustomerStatementLine> lines);

    int deleteLines(@Param("statementId") Long statementId);
}
