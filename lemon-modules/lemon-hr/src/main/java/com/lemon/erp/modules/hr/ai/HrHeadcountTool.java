package com.lemon.erp.modules.hr.ai;

import com.lemon.erp.framework.ai.AiSchema;
import com.lemon.erp.framework.ai.AiTool;
import com.lemon.erp.framework.ai.AiToolArgs;
import com.lemon.erp.framework.ai.AiToolResult;
import com.lemon.erp.modules.hr.dto.HeadcountRow;
import com.lemon.erp.modules.hr.service.HrEmployeeService;
import com.lemon.erp.modules.hr.service.HrEmployeeService.HeadcountGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 在职人数与结构、入离职统计：按部门、职位、用工类型或学历汇总，复用 {@link HrEmployeeService#headcount}（按数据权限）。
 * 只返回人数，不返回员工个人信息
 */
@Component
@RequiredArgsConstructor
public class HrHeadcountTool implements AiTool {

    /** 一组的人数；第一行为合计 */
    record HeadcountItem(String name, long active, long probation, long joined, long left) {
    }

    private final HrEmployeeService employeeService;

    @Override
    public String name() {
        return "hr_headcount";
    }

    @Override
    public String labelKey() {
        return "hr.ai.tool.headcount";
    }

    @Override
    public String descriptionKey() {
        return "hr.ai.tool.headcount.desc";
    }

    @Override
    public Map<String, Object> parameters() {
        AiSchema s = AiSchema.object().prop(HrAiSupport.GROUP_BY, AiSchema.enumOf("hr.ai.param.headcountGroupBy",
                HeadcountGroup.DEPT.name(), HeadcountGroup.POSITION.name(), HeadcountGroup.EMP_TYPE.name(), HeadcountGroup.EDUCATION.name()), false);
        return HrAiSupport.dateRange(s).build();
    }

    @Override
    public String permission() {
        return "hr:employee:list";
    }

    @Override
    public Object execute(AiToolArgs args) {
        HeadcountGroup group = HrAiSupport.enumArg(args, HrAiSupport.GROUP_BY, HeadcountGroup.class, HeadcountGroup.DEPT);
        List<HeadcountRow> list = employeeService.headcount(group, HrAiSupport.start(args), HrAiSupport.end(args));
        long[] sum = new long[4];
        List<HeadcountItem> rows = new ArrayList<>();
        rows.add(null);
        for (HeadcountRow r : list) {
            HeadcountItem item = new HeadcountItem(r.getGroupName(), n(r.getActive()), n(r.getProbation()), n(r.getJoined()), n(r.getLefted()));
            sum[0] += item.active();
            sum[1] += item.probation();
            sum[2] += item.joined();
            sum[3] += item.left();
            rows.add(item);
        }
        rows.set(0, new HeadcountItem(HrAiSupport.totalLabel(), sum[0], sum[1], sum[2], sum[3]));
        return AiToolResult.rows(rows);
    }

    private static long n(Long v) {
        return v == null ? 0 : v;
    }
}
