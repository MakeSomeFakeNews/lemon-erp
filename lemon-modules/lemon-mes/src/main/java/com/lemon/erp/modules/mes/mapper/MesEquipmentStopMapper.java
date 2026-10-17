package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesEquipmentStop;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface MesEquipmentStopMapper extends BaseMapper<MesEquipmentStop> {

    /** 与 [from, to) 有交叠的停机记录 */
    IPage<MesEquipmentStop> selectPageList(IPage<MesEquipmentStop> page, @Param("equipmentId") Long equipmentId,
                                           @Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                           @Param("reason") String reason);

    /** 与 [from, to) 有交叠的停机记录（OEE 计算） */
    List<MesEquipmentStop> selectOverlapping(@Param("equipmentId") Long equipmentId, @Param("from") LocalDateTime from,
                                             @Param("to") LocalDateTime to);

    /**
     * 设备报工汇总（OEE 的产量与理论工时）：equipmentId、reportDate、goodQty、scrapQty、idealHours（(合格 + 报废) × 工序标准工时）
     *
     * @param equipmentId 为空时为全部设备
     */
    List<Map<String, Object>> selectOutput(@Param("equipmentId") Long equipmentId, @Param("begin") LocalDate begin,
                                           @Param("end") LocalDate end);
}
