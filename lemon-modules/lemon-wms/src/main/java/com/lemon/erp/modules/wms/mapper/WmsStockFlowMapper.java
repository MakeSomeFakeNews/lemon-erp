package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.FlowQuery;
import com.lemon.erp.modules.wms.dto.StockFlow;
import com.lemon.erp.modules.wms.dto.StockFlowView;
import com.lemon.erp.modules.wms.dto.StockHistoryQuery;
import com.lemon.erp.modules.wms.dto.StockHistoryRow;
import com.lemon.erp.modules.wms.model.StockBizType;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 库存流水：只追加，不修改（委外件收货追加子件成本时只更新成本单价、成本金额）
 */
public interface WmsStockFlowMapper {

    void insertBatch(@Param("list") List<StockFlow> list);

    /**
     * @param begin 发生时间 &gt;= begin，可为空
     * @param end   发生时间 &lt; end，可为空
     */
    IPage<StockFlowView> selectPageList(IPage<StockFlowView> page, @Param("q") FlowQuery query,
                                        @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /**
     * 单据各物料的成本金额、数量合计（只含已计价的流水）
     *
     * @return [{materialId, qty, amount}]
     */
    List<Map<String, Object>> selectDocCosts(@Param("kind") StockBizType kind, @Param("docIds") Collection<Long> docIds);

    /**
     * 按来源单号汇总某类出入库单各物料的成本金额、数量合计（只含已计价的流水）
     *
     * @param bizType 入库类型 / 出库类型
     * @return [{materialId, qty, amount}]
     */
    List<Map<String, Object>> selectSourceCosts(@Param("kind") StockBizType kind, @Param("bizType") String bizType,
                                                @Param("sourceNo") String sourceNo);

    /** 调拨单流水没有业务单价的（同价调拨、未填调拨价）记为成本单价 */
    void fillTransferBizPrice(@Param("docId") Long docId);

    /** 单据某些物料已计价的流水（id、materialId、qty、costPrice、costAmount），按 ID 排序 */
    List<StockFlowView> selectPricedFlows(@Param("kind") StockBizType kind, @Param("docId") Long docId,
                                          @Param("materialIds") Collection<Long> materialIds);

    /**
     * 单据按库存位置汇总的成本金额（盘点单明细的差异金额）
     *
     * @return [{materialId, locationId, batchNo, amount}]
     */
    List<Map<String, Object>> selectSlotCosts(@Param("kind") StockBizType kind, @Param("docId") Long docId);

    void updateFlowCost(@Param("id") Long id, @Param("costPrice") BigDecimal costPrice, @Param("costAmount") BigDecimal costAmount);

    /**
     * 经营历史的期初：开始日期之前的数量、成本金额合计
     *
     * @return {qty, amount, estimated}，estimated 为 1 表示含升级前回填的近似成本
     */
    Map<String, Object> selectHistoryOpening(@Param("q") StockHistoryQuery query);

    /** 经营历史：日期范围内的流水，按业务日期、发生顺序 */
    List<StockHistoryRow> selectHistory(@Param("q") StockHistoryQuery query, @Param("limit") int limit);

    /** 物料是否发生过库存业务 */
    boolean existsByMaterialId(@Param("materialId") Long materialId);
}
