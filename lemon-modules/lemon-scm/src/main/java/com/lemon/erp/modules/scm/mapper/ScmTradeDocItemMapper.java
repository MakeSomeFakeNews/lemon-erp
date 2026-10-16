package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmTradeDocItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ScmTradeDocItemMapper extends BaseMapper<ScmTradeDocItem> {

    List<ScmTradeDocItem> selectByDocId(@Param("docId") Long docId);

    void insertBatch(@Param("list") List<ScmTradeDocItem> list);

    BigDecimal sumQtyByDocNo(@Param("docNo") String docNo, @Param("kind") String kind, @Param("materialId") Long materialId);
}
