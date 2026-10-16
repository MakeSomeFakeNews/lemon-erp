package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.BatchQuery;
import com.lemon.erp.modules.wms.dto.BatchView;
import com.lemon.erp.modules.wms.entity.WmsBatch;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WmsBatchMapper extends BaseMapper<WmsBatch> {

    /** 分页查询批次，带当前在库数量 */
    IPage<BatchView> selectPageList(IPage<BatchView> page, @Param("q") BatchQuery query);

    BatchView selectViewById(@Param("id") Long id);

    void insertBatch(@Param("list") List<WmsBatch> list);
}
