package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.TradeDocQuery;
import com.lemon.erp.modules.scm.entity.ScmTradeDoc;
import org.apache.ibatis.annotations.Param;

public interface ScmTradeDocMapper extends BaseMapper<ScmTradeDoc> {

    IPage<ScmTradeDoc> selectPageList(IPage<ScmTradeDoc> page, @Param("kind") String kind, @Param("q") TradeDocQuery query);

    ScmTradeDoc selectDetail(@Param("id") Long id);
}
