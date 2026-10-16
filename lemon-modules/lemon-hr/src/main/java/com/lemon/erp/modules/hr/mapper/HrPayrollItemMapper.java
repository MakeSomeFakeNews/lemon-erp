package com.lemon.erp.modules.hr.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.hr.entity.HrPayrollItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface HrPayrollItemMapper extends BaseMapper<HrPayrollItem> {

    /** @param keyword 工号或姓名 */
    List<HrPayrollItem> selectByPayroll(@Param("payrollId") Long payrollId, @Param("keyword") String keyword);

    /** 员工的工资条：已确认、已发放的工资表 */
    List<HrPayrollItem> selectPayslips(@Param("employeeId") Long employeeId);

    void insertBatch(@Param("items") List<HrPayrollItem> items);

    /** 员工本年此前最近一个月的累计应纳税所得额、累计已扣税额：[{employeeId, cumTaxable, cumTax}] */
    List<Map<String, Object>> selectPriorCumulative(@Param("employeeIds") java.util.Collection<Long> employeeIds,
                                                    @Param("yearStart") int yearStart, @Param("period") int period);

    /** 计提凭证：按部门汇总 应发 + 单位社保公积金 */
    List<Map<String, Object>> selectCostByDept(@Param("payrollId") Long payrollId);

    /** 发放凭证：其他扣款合计 */
    BigDecimal sumOtherDeduct(@Param("payrollId") Long payrollId);

    /**
     * 计件工资：期间内报工的计件工资按操作员（员工的登录账号）汇总
     *
     * @return [{employeeId, amount}]
     */
    List<Map<String, Object>> selectPieceWages(@Param("begin") LocalDate begin, @Param("end") LocalDate end);
}
