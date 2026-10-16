package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.wms.dto.SerialQuery;
import com.lemon.erp.modules.wms.dto.SerialView;
import com.lemon.erp.modules.wms.entity.WmsSerial;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface WmsSerialMapper extends BaseMapper<WmsSerial> {

    void insertBatch(@Param("list") List<WmsSerial> list);

    /** 批量更新序列号的位置和状态 */
    void updateSlot(@Param("ids") Collection<Long> ids, @Param("warehouseId") Long warehouseId,
                    @Param("locationId") long locationId, @Param("batchNo") String batchNo, @Param("status") String status);

    IPage<SerialView> selectPageList(IPage<SerialView> page, @Param("q") SerialQuery query);

    SerialView selectViewById(@Param("id") Long id);
}
