package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.dto.IoRequests;
import com.lemon.erp.modules.fin.entity.FinIoDoc;
import com.lemon.erp.modules.fin.entity.FinIoDocItem;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.IoStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinIoDocMapper extends BaseMapper<FinIoDoc> {

    IPage<FinIoDoc> selectPageList(IPage<FinIoDoc> page, @Param("q") IoRequests.Query query);

    /** 各单据明细的类别名称（docId、categoryName，去重） */
    List<FinIoDocItem> selectCategoryNames(@Param("docIds") List<Long> docIds);

    FinIoDoc selectDetail(@Param("id") Long id);

    /** 明细（含类别名称、科目） */
    List<FinIoDocItem> selectItems(@Param("docId") Long docId);

    /** 已确认单据按收支类别汇总 */
    List<IoRequests.SummaryRow> selectSummary(@Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                              @Param("direction") FundDirection direction, @Param("status") IoStatus status);

    /**
     * 已确认单据的明细行，以及账户调拨的手续费
     *
     * @param docs        包含收支单的明细行（选了类别时只有该类别）
     * @param transferFee 包含账户调拨的手续费
     */
    IPage<IoRequests.LineRow> selectLines(IPage<IoRequests.LineRow> page, @Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                          @Param("direction") FundDirection direction, @Param("categoryId") Long categoryId,
                                          @Param("status") IoStatus status, @Param("docs") boolean docs,
                                          @Param("transferFee") boolean transferFee);
}
