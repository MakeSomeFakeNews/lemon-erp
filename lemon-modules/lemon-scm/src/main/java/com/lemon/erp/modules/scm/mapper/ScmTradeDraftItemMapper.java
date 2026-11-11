package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmTradeDraftItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmTradeDraftItemMapper extends BaseMapper<ScmTradeDraftItem> {

    /** 草稿明细（带物料、库位信息），按行号 */
    List<ScmTradeDraftItem> selectByDraftId(@Param("draftId") Long draftId);

    void insertBatch(@Param("list") List<ScmTradeDraftItem> list);
}
