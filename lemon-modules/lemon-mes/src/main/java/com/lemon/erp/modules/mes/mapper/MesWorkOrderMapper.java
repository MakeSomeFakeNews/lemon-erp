package com.lemon.erp.modules.mes.mapper;

import com.lemon.erp.modules.mes.model.WorkOrderStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.dto.WorkOrderQuery;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MesWorkOrderMapper extends BaseMapper<MesWorkOrder> {

    IPage<MesWorkOrder> selectPageList(IPage<MesWorkOrder> page, @Param("q") WorkOrderQuery query);

    MesWorkOrder selectDetail(@Param("id") Long id);

    /**
     * 未结束工单的未完工数量（工单数量 − 完工 − 报工报废），按产品（MRP 在制）
     *
     * @return [{materialId, qty}]
     */
    List<Map<String, Object>> selectOpenOutput(@Param("statuses") Collection<WorkOrderStatus> statuses,
                                               @Param("materialIds") Collection<Long> materialIds);

    /** 可承诺量：在制工单的未完工产出（正数）、工单未领用料（负数）：[{date, type, docNo, qty}] */
    List<Map<String, Object>> selectAtpLines(@Param("materialId") Long materialId, @Param("warehouseId") Long warehouseId);

    /** 各物料在制工单的未完工产出减去工单未领用料：[{materialId, qty}] */
    List<Map<String, Object>> selectNetSupply(@Param("materialIds") Collection<Long> materialIds);

    /** 物料的供应方式（生产计划属性，没有设置时有默认 BOM 为自制）：[{materialId, supplyType}] */
    List<Map<String, Object>> selectSupplyTypes(@Param("materialIds") Collection<Long> materialIds);

    /** 已下达 / 生产中的工单：完工入库满数量，或已有完工且 完工 + 报工报废 达到工单数量时改为已完工 */
    int markCompleted(@Param("id") Long id);

    /** 累计报工报废数量 */
    int addScrap(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 回写完工数量：不超过计划数量 */
    int addFinished(@Param("id") Long id, @Param("qty") BigDecimal qty);

    /** 第一道工序在该工作中心的未结束工单，数据权限部门改为工作中心的所属车间 */
    int updateOpenDeptByFirstWorkCenter(@Param("workCenterId") Long workCenterId, @Param("deptId") Long deptId);
}
