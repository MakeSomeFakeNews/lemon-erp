package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.TransferSummaryRow;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.model.StockBizType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WmsStockDocMapper extends BaseMapper<WmsStockDoc> {

    /** 分页查询某种单据，带仓库、制单人、确认人名称 */
    IPage<WmsStockDoc> selectPageList(IPage<WmsStockDoc> page, @Param("kind") StockBizType kind, @Param("q") DocQuery query,
                                      @Param("today") java.time.LocalDate today);

    /** 各单据明细的仓库（warehouseId 为空表示表头仓库）：[{docId, warehouseId, warehouseName}] */
    java.util.List<java.util.Map<String, Object>> selectItemWarehouses(@Param("docIds") java.util.Collection<Long> docIds);

    WmsStockDoc selectDetail(@Param("id") Long id);

    /** 加行锁读取单据（在途调拨的验收、差异处理串行执行） */
    WmsStockDoc selectForUpdate(@Param("id") Long id);

    /** 关联到原调拨单的补发、多发补调的调拨单（id、单据号、状态、业务日期、合计数量） */
    List<WmsStockDoc> selectByOrigin(@Param("originId") Long originId);

    /** 调拨汇总：按调出仓库 → 调入仓库汇总已调出的调拨单（含红字） */
    List<TransferSummaryRow> selectTransferSummary(@Param("q") DocQuery query);
}
