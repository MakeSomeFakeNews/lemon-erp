package com.lemon.erp.modules.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pm.entity.PmRevenue;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 项目月结：入账的人工、费用和完工百分比确认收入
 */
public interface PmCloseMapper extends BaseMapper<PmRevenue> {

    /**
     * 未入账的已审批工时成本，按项目和员工岗位类别汇总
     *
     * @return [{projectId, category, amount}]
     */
    List<Map<String, Object>> selectUnpostedLabor(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 未入账的已审批手工费用（领料成本已由成本核算计入合同履约成本），按项目汇总
     *
     * @return [{projectId, amount}]
     */
    List<Map<String, Object>> selectUnpostedExpense(@Param("begin") LocalDate begin, @Param("end") LocalDate end);

    /**
     * 有合同金额、已启动的项目截至期末的累计实际成本（人工 + 费用 + 领料）与已确认的收入、已结转的成本
     *
     * @return [{projectId, code, customerId, contractAmount, taxRate, progress, status, cumCost, recognizedRevenue, recognizedCost}]
     */
    List<Map<String, Object>> selectRecognition(@Param("end") LocalDate end);

    int markTimesheets(@Param("voucherId") Long voucherId, @Param("projectId") Long projectId,
                       @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    int markCosts(@Param("voucherId") Long voucherId, @Param("projectId") Long projectId,
                  @Param("begin") LocalDate begin, @Param("end") LocalDate end);

    List<PmRevenue> selectByProject(@Param("projectId") Long projectId);
}
