package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinSupplierStatement;
import com.lemon.erp.modules.fin.entity.FinSupplierStatementLine;
import com.lemon.erp.modules.fin.model.StatementStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinSupplierStatementMapper extends BaseMapper<FinSupplierStatement> {

    /** @param supplierId 供应商门户只看本供应商；门户不显示草稿 */
    IPage<FinSupplierStatement> selectPageList(IPage<FinSupplierStatement> page, @Param("supplierId") Long supplierId,
                                               @Param("status") StatementStatus status, @Param("keyword") String keyword,
                                               @Param("portal") boolean portal);

    FinSupplierStatement selectDetail(@Param("id") Long id);

    List<FinSupplierStatementLine> selectLines(@Param("statementId") Long statementId);

    /** 期间内还没有对过账的应付单 */
    List<FinSupplierStatementLine> selectUnreconciledBills(@Param("supplierId") Long supplierId, @Param("begin") LocalDate begin,
                                                           @Param("end") LocalDate end);

    int insertLines(@Param("lines") List<FinSupplierStatementLine> lines);

    int deleteLines(@Param("statementId") Long statementId);
}
