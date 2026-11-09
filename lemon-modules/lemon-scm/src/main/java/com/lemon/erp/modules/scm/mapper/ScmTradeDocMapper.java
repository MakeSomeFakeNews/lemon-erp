package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.TradeDocQuery;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import com.lemon.erp.modules.scm.model.TradeKind;
import org.apache.ibatis.annotations.Param;

public interface ScmTradeDocMapper extends BaseMapper<ScmTradeDoc> {

    IPage<ScmTradeDoc> selectPageList(IPage<ScmTradeDoc> page, @Param("kind") TradeKind kind, @Param("q") TradeDocQuery query);

    ScmTradeDoc selectDetail(@Param("id") Long id);

    /** 各单据明细的仓库（warehouseId 为空表示表头仓库）：[{docId, warehouseId, warehouseName}] */
    java.util.List<java.util.Map<String, Object>> selectItemWarehouses(@Param("docIds") java.util.Collection<Long> docIds);
}
