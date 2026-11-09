package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmTradeDocItem;
import com.lemon.erp.modules.scm.model.TradeKind;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ScmTradeDocItemMapper extends BaseMapper<ScmTradeDocItem> {

    List<ScmTradeDocItem> selectByDocId(@Param("docId") Long docId);

    void insertBatch(@Param("list") List<ScmTradeDocItem> list);

    /**
     * 订单明细最近一张入库 / 出库单（按种类）对应的仓库单据：[{orderItemId, wmsDocId}]，关闭订单时把采购的尾差调整追加到该入库单的成本
     */
    List<java.util.Map<String, Object>> selectLastWmsDocs(@Param("orderItemIds") java.util.Collection<Long> orderItemIds, @Param("kind") TradeKind kind);

    BigDecimal sumQtyByDocNo(@Param("docNo") String docNo, @Param("kind") TradeKind kind, @Param("materialId") Long materialId);
}
