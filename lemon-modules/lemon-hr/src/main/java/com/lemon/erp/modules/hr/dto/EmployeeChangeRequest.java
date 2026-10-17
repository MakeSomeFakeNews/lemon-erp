package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.model.ChangeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 人事异动
 *
 * @param type     REGULAR 转正 / TRANSFER 调岗 / SALARY 调薪 / LEAVE 离职
 * @param deptId   调岗后的部门
 * @param positionId 调岗后的职位
 * @param salary   调薪后的基本工资
 * @param handoverUserId 离职交接人：负责的客户、商机、项目、审批待办移交给他；为空时客户退回公海
 */
public record EmployeeChangeRequest(
        @NotNull(message = "{hr.valid.selectChangeType}") ChangeType type,
        @NotNull(message = "{hr.valid.selectEffectiveDate}") LocalDate changeDate,
        Long deptId,
        Long positionId,
        @DecimalMin(value = "0", message = "{hr.valid.baseSalaryCannotLess}") @Digits(integer = 16, fraction = 2, message = "{hr.valid.baseSalaryHaveMostDecimalPlaces}") BigDecimal salary,
        @Size(max = 255, message = "{hr.valid.descriptionCannotExceedCharacters}") String reason,
        Long handoverUserId) {
}
