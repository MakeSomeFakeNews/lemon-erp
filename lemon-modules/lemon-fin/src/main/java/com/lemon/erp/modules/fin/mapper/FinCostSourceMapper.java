package com.lemon.erp.modules.fin.mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 成本核算读取仓库、供应链、生产的业务数据（只读，按业务日期范围汇总）。
 * 财务是最下游的模块，报表类查询直接关联上游表，不在上游模块增加只为财务服务的接口
 */
public interface FinCostSourceMapper {

    /**
     * 库存出入数量（不含调拨），按物料、出入库类型（字典 wms_in_type / wms_out_type，盘点为 CHECK）汇总
     *
     * @param begin 为空时从最早开始
     * @return [{materialId, bizType, inQty, outQty}]
     */
    List<Map<String, Object>> selectStockMovements(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 填写了收发类别的出入库单（其他入库、其他出库、报废出库）按类别汇总的数量，不含供应商寄售仓
     *
     * @return [{materialId, kind（IN / OUT）, ioCategory, qty（入库为入库数量、出库为出库数量，红字为负）}]
     */
    List<Map<String, Object>> selectIoCategoryMovements(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 采购入库 / 退货、销售出库 / 退货，按物料、种类汇总数量和不含税金额
     *
     * @return [{materialId, kind, qty, net}]
     */
    List<Map<String, Object>> selectTradeSums(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 工单领料 / 退料 / 完工入库，按工单、物料、种类汇总数量
     *
     * @return [{woId, materialId, kind, qty}]
     */
    List<Map<String, Object>> selectWoDocSums(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 报工工时与按工作中心费率计算的人工、制造费用，按工单汇总
     *
     * @return [{woId, hours, labor, overhead}]
     */
    List<Map<String, Object>> selectReportCosts(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 工单基本信息与期初前累计完工数量
     *
     * @return [{id, woNo, materialId, qty, status, finishedBefore}]
     */
    List<Map<String, Object>> selectWorkOrders(@Param("ids") List<Long> ids, @Param("begin") LocalDate begin);

    /**
     * 委外收货核销的用料
     *
     * @return [{productId, materialId, qty}]
     */
    List<Map<String, Object>> selectOutsourceConsumption(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 移动加权平均、先进先出物料的出入库明细，按业务日期、发生顺序排列
     *
     * @return [{materialId, bizDate, seq, bizType, docKind（IN / OUT，盘点为空）, qty（入库为正）, tradeQty, tradeNet（采购入库 / 退货的不含税金额）, woId（完工入库）}]
     */
    List<Map<String, Object>> selectCostEvents(@Param("begin") LocalDate begin, @Param("end") LocalDate end,
                                               @Param("materialIds") Collection<Long> materialIds);

    /**
     * 已完成的组装拆卸单及其子件
     *
     * @return [{assemblyNo, kind, productId, qty, partId, partQty}]
     */
    List<Map<String, Object>> selectAssemblies(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /** 参考成本价大于 0 的物料：[{materialId, refCost}] */
    List<Map<String, Object>> selectRefCosts();
}
