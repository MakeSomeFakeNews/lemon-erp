package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsCheckItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WmsCheckItemMapper extends BaseMapper<WmsCheckItem> {

    /** 盘点明细，带物料、库位信息 */
    List<WmsCheckItem> selectByCheckId(@Param("checkId") Long checkId);

    void insertBatch(@Param("list") List<WmsCheckItem> list);
}
