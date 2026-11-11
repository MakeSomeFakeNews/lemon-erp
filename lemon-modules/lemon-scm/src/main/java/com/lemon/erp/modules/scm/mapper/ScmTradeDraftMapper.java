package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.TradeDocQuery;
import com.lemon.erp.modules.scm.entity.ScmTradeDraft;
import com.lemon.erp.modules.scm.model.TradeKind;
import org.apache.ibatis.annotations.Param;

public interface ScmTradeDraftMapper extends BaseMapper<ScmTradeDraft> {

    IPage<ScmTradeDraft> selectPageList(IPage<ScmTradeDraft> page, @Param("kind") TradeKind kind, @Param("q") TradeDocQuery query);

    ScmTradeDraft selectDetail(@Param("id") Long id);
}
