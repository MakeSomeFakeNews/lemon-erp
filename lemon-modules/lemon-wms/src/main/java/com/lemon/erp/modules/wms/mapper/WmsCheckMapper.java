package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.entity.WmsCheck;
import org.apache.ibatis.annotations.Param;

public interface WmsCheckMapper extends BaseMapper<WmsCheck> {

    /** 分页查询盘点单，带明细行数和差异行数 */
    IPage<WmsCheck> selectPageList(IPage<WmsCheck> page, @Param("q") DocQuery query);

    WmsCheck selectDetail(@Param("id") Long id);
}
