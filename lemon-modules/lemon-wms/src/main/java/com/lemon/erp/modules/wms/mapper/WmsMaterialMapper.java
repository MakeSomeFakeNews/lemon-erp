package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import org.apache.ibatis.annotations.Param;

public interface WmsMaterialMapper extends BaseMapper<WmsMaterial> {

    /** 锁定物料行（SELECT ... FOR UPDATE），串行化同一物料的并发预留；在事务中调用 */
    WmsMaterial selectForUpdate(@Param("id") Long id);
}
