package com.lemon.erp.modules.hr.dto;

import com.lemon.erp.modules.hr.model.EmployeeStatus;
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
        @NotBlank(message = "{hr.valid.employeeNoCannotEmpty}")
        @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "{hr.valid.employeeNoMayOnlyContainLetters}") String empNo,
        @NotBlank(message = "{hr.valid.nameRequired}") @Size(max = 64, message = "{hr.valid.nameCannotExceedCharacters}") String name,
        @Pattern(regexp = "M|F", message = "{hr.valid.invalidGender}") String gender,
        LocalDate birthDate,
        @Pattern(regexp = "^$|^\\d{17}[\\dXx]$", message = "{hr.valid.invalidIdCardNumber}") String idCard,
        @Size(max = 32, message = "{hr.valid.mobileNumberCannotExceedCharacters}") String phone,
        @Email(message = "{hr.valid.invalidEmailFormat}") @Size(max = 128, message = "{hr.valid.emailCannotExceedCharacters}") String email,
        String education,
        @NotNull(message = "{hr.valid.selectDepartment}") Long deptId,
        Long positionId,
        Long userId,
        Long shiftId,
        String empType,
        EmployeeStatus status,
        @NotNull(message = "{hr.valid.selectHireDate}") LocalDate entryDate,
        LocalDate regularDate,
        @Size(max = 255, message = "{hr.valid.addressCannotExceedCharacters}") String address,
        @Size(max = 64, message = "{hr.valid.emergencyContactCannotExceedCharacters}") String emergencyContact,
        @Size(max = 32, message = "{hr.valid.emergencyPhoneCannotExceedCharacters}") String emergencyPhone,
        @Size(max = 255, message = "{hr.valid.remarkCannotExceedCharacters}") String remark,
        LocalDate workStartDate) {
}
