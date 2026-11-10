-- ====================================================
-- 工程变更审批：流程变量增加 itemChanges 用料变更项数、opChanges 工序变更项数（代码中声明，不需要改库）；
-- 内置示例流程「工程变更审批」（mes_ecn）增加条件分支：变更原因为客户要求、质量问题或用料变更 ≥ 5 项时总经理审批。
-- 只更新从未发布、从未修改过的内置模型（各租户按角色编码换成本租户的岗位角色），已调整或已发布的模型保持不变。可重复执行
-- ====================================================

SET @ecn_design = '{"form": [], "flow": {"id": "start", "type": "START", "name": "发起人", "next": {"id": "leader", "type": "APPROVER", "name": "部门主管审批", "assignee": {"type": "DEPT_LEADER", "ids": [], "names": [], "level": 1}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "qa", "type": "APPROVER", "name": "质量工程师审核", "assignee": {"type": "ROLE", "ids": [{QA}], "names": ["质量工程师"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": {"id": "impact_check", "type": "CONDITION", "name": "按变更原因和影响", "next": {"id": "cc_plan", "type": "CC", "name": "抄送生产计划员", "assignee": {"type": "ROLE", "ids": [{PLANNER}], "names": ["生产计划员"]}, "next": null}, "branches": [{"id": "impact_check_yes", "name": "客户要求 / 质量问题或用料变更 ≥ 5 项", "defaultBranch": false, "conditions": [[{"field": "reason", "operator": "IN", "value": "CUSTOMER,QUALITY"}], [{"field": "itemChanges", "operator": "GE", "value": "5"}]], "next": {"id": "gm", "type": "APPROVER", "name": "总经理审批", "assignee": {"type": "ROLE", "ids": [{GM}], "names": ["总经理"]}, "approveMode": "OR", "emptyAction": "TO_ADMIN", "next": null}}, {"id": "impact_check_else", "name": "其他情况", "defaultBranch": true, "conditions": [], "next": null}]}}}}}';

UPDATE wf_model m
JOIN sys_role q ON q.tenant_id = m.tenant_id AND q.code = 'quality'
JOIN sys_role g ON g.tenant_id = m.tenant_id AND g.code = 'gm'
JOIN sys_role p ON p.tenant_id = m.tenant_id AND p.code = 'planner'
SET m.design_json = REPLACE(REPLACE(REPLACE(@ecn_design, '{QA}', q.id), '{GM}', g.id), '{PLANNER}', p.id),
    m.remark = '内置流程（未发布）：部门主管 → 质量工程师 → 变更原因为客户要求、质量问题或用料变更 ≥ 5 项时总经理 → 抄送生产计划员。发布后工程变更提交需审批，通过后按生效日期切换 BOM；流程变量 reason、itemChanges、opChanges、materialCode'
WHERE m.model_key = 'mes_ecn' AND m.version IS NULL
  AND (m.update_time IS NULL OR m.update_time = m.create_time)
  AND m.design_json NOT LIKE '%itemChanges%';
