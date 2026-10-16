package com.lemon.erp.modules.hr.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 新增 / 修改员工档案。修改时部门、职位、状态、入职日期不变，通过人事异动调整
 *
 * @param status      入职时的状态 PROBATION 试用 / REGULAR 正式，默认试用
 * @param regularDate 试用期员工的计划转正日期，默认入职日期 + 3 个月
 * @param workStartDate 参加工作日期（累计工龄，计算法定年假），为空时按入职日期
 */
public record EmployeeRequest(
        @NotBlank(message = "工号不能为空")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "工号只能包含字母、数字、下划线和短横线，最多32个字符") String empNo,
        @NotBlank(message = "姓名不能为空") @Size(max = 64, message = "姓名不能超过64个字符") String name,
        @Pattern(regexp = "M|F", message = "性别不正确") String gender,
        LocalDate birthDate,
        @Pattern(regexp = "^$|^\\d{17}[\\dXx]$", message = "身份证号格式不正确") String idCard,
        @Size(max = 32, message = "手机号不能超过32个字符") String phone,
        @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱不能超过128个字符") String email,
        String education,
        @NotNull(message = "请选择部门") Long deptId,
        Long positionId,
        Long userId,
        Long shiftId,
        String empType,
        String status,
        @NotNull(message = "请选择入职日期") LocalDate entryDate,
        LocalDate regularDate,
        @Size(max = 255, message = "住址不能超过255个字符") String address,
        @Size(max = 64, message = "紧急联系人不能超过64个字符") String emergencyContact,
        @Size(max = 32, message = "紧急联系电话不能超过32个字符") String emergencyPhone,
        @Size(max = 255, message = "备注不能超过255个字符") String remark,
        LocalDate workStartDate) {
}
