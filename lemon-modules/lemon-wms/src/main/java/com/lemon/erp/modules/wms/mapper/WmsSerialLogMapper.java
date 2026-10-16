package com.lemon.erp.modules.wms.mapper;

import com.lemon.erp.modules.wms.dto.SerialLog;
import com.lemon.erp.modules.wms.dto.SerialLogView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 序列号轨迹：只追加，不修改
 */
public interface WmsSerialLogMapper {

    void insertBatch(@Param("list") List<SerialLog> list);

    List<SerialLogView> selectBySerial(@Param("materialId") Long materialId, @Param("serialNo") String serialNo);
}
