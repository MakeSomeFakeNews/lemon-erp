package com.lemon.erp.modules.workflow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.framework.tenant.TenantContext;
import com.lemon.erp.framework.tenant.TenantInitializer;
import com.lemon.erp.modules.workflow.entity.WfModel;
import com.lemon.erp.modules.workflow.mapper.WfModelMapper;
import com.lemon.erp.modules.workflow.model.AssigneeConfig;
import com.lemon.erp.modules.workflow.model.AssigneeType;
import com.lemon.erp.modules.workflow.model.FlowBranch;
import com.lemon.erp.modules.workflow.model.FlowDesign;
import com.lemon.erp.modules.workflow.model.FlowNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 新租户复制平台的流程模型（未发布的草稿），审批角色换成新租户中复制出的同名岗位角色；
 * 指定成员、平台自建角色在新租户中不存在，清空后由租户管理员在设计器中重新选择再发布。
 * 业务流程未发布前，对应业务不走审批
 */
@Component
@RequiredArgsConstructor
public class WfTenantInitializer implements TenantInitializer {

    private final WfModelMapper modelMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void initialize(Context context) {
        List<WfModel> models = TenantContext.callAs(TenantContext.PLATFORM_TENANT_ID,
                () -> modelMapper.selectList(Wrappers.<WfModel>lambdaQuery().orderByAsc(WfModel::getSort)));
        for (WfModel model : models) {
            TenantInitializer.asNew(model);
            model.setVersion(null);
            model.setDefinitionId(null);
            model.setDeployTime(null);
            if (model.getDesignJson() != null) {
                FlowDesign design = objectMapper.readValue(model.getDesignJson(), FlowDesign.class);
                model.setDesignJson(objectMapper.writeValueAsString(
                        new FlowDesign(design.form(), remap(design.flow(), context.roleIdMap()))));
            }
            modelMapper.insert(model);
        }
    }

    private static FlowNode remap(FlowNode node, Map<Long, Long> roleIdMap) {
        if (node == null) {
            return null;
        }
        List<FlowBranch> branches = node.branches().stream()
                .map(b -> new FlowBranch(b.id(), b.name(), b.defaultBranch(), b.conditions(), remap(b.next(), roleIdMap)))
                .toList();
        return new FlowNode(node.id(), node.type(), node.name(), remap(node.assignee(), roleIdMap), node.approveMode(),
                node.emptyAction(), branches, remap(node.next(), roleIdMap));
    }

    private static AssigneeConfig remap(AssigneeConfig assignee, Map<Long, Long> roleIdMap) {
        if (assignee == null || assignee.type() == AssigneeType.USER) {
            return assignee == null ? null : new AssigneeConfig(AssigneeType.USER, List.of(), List.of(), assignee.level());
        }
        if (assignee.type() != AssigneeType.ROLE) {
            return assignee;
        }
        List<Long> ids = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < assignee.ids().size(); i++) {
            Long mapped = roleIdMap.get(assignee.ids().get(i));
            if (mapped != null) {
                ids.add(mapped);
                if (assignee.names() != null && i < assignee.names().size()) {
                    names.add(assignee.names().get(i));
                }
            }
        }
        return new AssigneeConfig(AssigneeType.ROLE, ids, names, assignee.level());
    }
}
