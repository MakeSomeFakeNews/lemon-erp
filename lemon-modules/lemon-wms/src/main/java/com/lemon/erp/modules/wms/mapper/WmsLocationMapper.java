package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.entity.WmsLocation;
import org.apache.ibatis.annotations.Param;

public interface WmsLocationMapper extends BaseMapper<WmsLocation> {

    /**
     * 分页查询库位，带仓库名称
     *
     * @param keyword 库位编码或名称，模糊匹配
     */
    IPage<WmsLocation> selectPageList(IPage<WmsLocation> page, @Param("warehouseId") Long warehouseId,
                                      @Param("keyword") String keyword, @Param("status") Integer status);
}
