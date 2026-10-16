package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.FlowQuery;
import com.lemon.erp.modules.wms.dto.StockFlow;
import com.lemon.erp.modules.wms.dto.StockFlowView;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存流水：只追加，不修改
 */
public interface WmsStockFlowMapper {

    void insertBatch(@Param("list") List<StockFlow> list);

    /**
     * @param begin 发生时间 &gt;= begin，可为空
     * @param end   发生时间 &lt; end，可为空
     */
    IPage<StockFlowView> selectPageList(IPage<StockFlowView> page, @Param("q") FlowQuery query,
                                        @Param("begin") LocalDateTime begin, @Param("end") LocalDateTime end);

    /** 物料是否发生过库存业务 */
    boolean existsByMaterialId(@Param("materialId") Long materialId);
}
