package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesEquipment;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface MesEquipmentMapper extends BaseMapper<MesEquipment> {

    /**
     * @param keyword  编码或名称
     * @param dueBefore 下次保养日期不晚于该日期（保养到期提醒）
     */
    IPage<MesEquipment> selectPageList(IPage<MesEquipment> page, @Param("keyword") String keyword,
                                       @Param("status") EquipmentStatus status, @Param("workCenterId") Long workCenterId,
                                       @Param("dueBefore") LocalDate dueBefore);
}
