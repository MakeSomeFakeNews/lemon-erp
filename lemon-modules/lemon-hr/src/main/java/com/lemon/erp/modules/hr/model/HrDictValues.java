package com.lemon.erp.modules.hr.model;

/**
 * 人力资源业务字典（租户可新增取值，字段保持字符串）中程序依赖的内置取值
 */
public final class HrDictValues {

    /** 用工类型（字典 hr_emp_type）：全职，未填写时的默认值 */
    public static final String EMP_TYPE_FULL_TIME = "FULL_TIME";

    /** 合同类型（字典 hr_contract_type）：无固定期限，没有结束日期；其他类型（含租户新增的）必须填结束日期 */
    public static final String CONTRACT_TYPE_OPEN = "OPEN";

    /*
     * 假期类型（字典 hr_leave_type）。以下内置值有特殊处理：年假、调休提交时校验余额，
     * 事假全额扣发、病假按比例扣发工资（HrPayrollService）。其他类型（婚假、产假及租户新增的类型）
     * 按普通带薪假处理：不校验余额、不扣工资，已通过的请假在考勤汇总中同样不计旷工
     */
    /** 年假 */
    public static final String LEAVE_ANNUAL = "ANNUAL";
    /** 事假 */
    public static final String LEAVE_PERSONAL = "PERSONAL";
    /** 病假 */
    public static final String LEAVE_SICK = "SICK";
    /** 调休 */
    public static final String LEAVE_COMPENSATORY = "COMPENSATORY";

    private HrDictValues() {
    }
}
