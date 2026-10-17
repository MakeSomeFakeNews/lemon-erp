package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.dto.ConsumableLedgerRow;
import com.lemon.erp.modules.fin.entity.FinConsumableUse;
import com.lemon.erp.modules.fin.entity.FinConsumableUseItem;
import com.lemon.erp.modules.fin.model.ConsumableStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface FinConsumableUseMapper extends BaseMapper<FinConsumableUse> {

    IPage<FinConsumableUse> selectPageList(IPage<FinConsumableUse> page, @Param("status") ConsumableStatus status,
                                           @Param("deptId") Long deptId, @Param("keyword") String keyword);

    FinConsumableUse selectDetail(@Param("id") Long id);

    List<FinConsumableUseItem> selectItems(@Param("useId") Long useId);

    int insertItems(@Param("items") List<FinConsumableUseItem> items);

    int deleteItems(@Param("useId") Long useId);

    /**
     * 领用台账：期间内已出库的领用按部门、物料汇总
     *
     * @return [{deptId, deptName, materialId, materialCode, materialName, unit, qty, refAmount}]
     */
    List<ConsumableLedgerRow> selectLedger(@Param("begin") LocalDate begin, @Param("end") LocalDate end, @Param("deptId") Long deptId);

    /**
     * 成本核算：期间内已出库的领用按物料、部门、费用科目汇总数量
     *
     * @return [{materialId, deptId, accountId, qty}]
     */
    List<Map<String, Object>> selectForCost(@Param("begin") LocalDate begin, @Param("end") LocalDate end);
}
