package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesMaintenance;
import com.lemon.erp.modules.mes.model.MaintenanceStatus;
import com.lemon.erp.modules.mes.model.MaintenanceType;
import org.apache.ibatis.annotations.Param;

public interface MesMaintenanceMapper extends BaseMapper<MesMaintenance> {

    /** @param keyword 维护单号、设备编码或名称 */
    IPage<MesMaintenance> selectPageList(IPage<MesMaintenance> page, @Param("equipmentId") Long equipmentId,
                                         @Param("type") MaintenanceType type, @Param("status") MaintenanceStatus status,
                                         @Param("keyword") String keyword);
}
