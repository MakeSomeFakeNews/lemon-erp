package com.lemon.erp.modules.workflow.service;

import com.lemon.erp.common.i18n.I18n;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.workflow.FlowBusiness;
import com.lemon.erp.modules.workflow.dto.StartFormView;
import com.lemon.erp.modules.workflow.engine.FlowTenant;
import com.lemon.erp.modules.workflow.engine.bpmn.BpmnModelConverter;
import com.lemon.erp.modules.workflow.engine.bpmn.FlowDesignValidator;
import com.lemon.erp.modules.workflow.engine.meta.FlowMetaService;
import com.lemon.erp.modules.workflow.entity.WfModel;
import com.lemon.erp.modules.workflow.mapper.WfModelMapper;
import com.lemon.erp.modules.workflow.model.FlowDesign;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 流程模型：维护流程设计草稿，发布为 Flowable 流程定义
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WfModelService extends ServiceImpl<WfModelMapper, WfModel> {

    private final RepositoryService repositoryService;
    private final FlowDesignValidator designValidator;
    private final BpmnModelConverter bpmnConverter;
    private final FlowMetaService metaService;
    private final ObjectMapper objectMapper;
    /** 业务模块声明的接入审批的流程 */
    private final List<FlowBusiness> businesses;

    public PageResult<WfModel> page(long current, long size, String name, String category) {
        Page<WfModel> page = Page.of(current, size);
        getBaseMapper().selectPageList(page, name, category);
        page.getRecords().forEach(m -> {
            FlowBusiness b = business(m.getModelKey());
            m.setBusinessName(b == null ? null : I18n.t(b.name()));
        });
        return PageResult.of(page);
    }

    /** 流程标识对应的业务，不是业务单据的流程返回 null */
    public FlowBusiness business(String modelKey) {
        return businesses.stream().filter(b -> b.processKey().equals(modelKey)).findFirst().orElse(null);
    }

    /** 业务名称、变量名称按请求语言翻译（输出给前端时使用） */
    public static FlowBusiness translate(FlowBusiness b) {
        if (b == null) {
            return null;
        }
        return new FlowBusiness(b.processKey(), I18n.t(b.name()), b.variables().stream()
                .map(v -> new FlowBusiness.Variable(v.field(), I18n.t(v.label()), v.type(), v.dictType(), v.options()))
                .toList());
    }

    public WfModel detail(Long id) {
        WfModel model = getExisting(id);
        FlowDesign design = readDesign(model);
        model.setDesigned(design != null);
        model.setForm(design == null ? List.of() : design.form());
        model.setFlow(design == null ? null : design.flow());
        model.setBusiness(translate(business(model.getModelKey())));
        return model;
    }

    /** @return 新模型的 ID */
    public Long create(WfModel model) {
        checkKeyUnique(model.getModelKey(), null);
        WfModel entity = new WfModel();
        copyBasic(model, entity);
        entity.setStatus(1);
        save(entity);
        return entity.getId();
    }

    public void update(Long id, WfModel model) {
        WfModel old = getExisting(id);
        if (old.getVersion() != null && !old.getModelKey().equals(model.getModelKey())) {
            throw new BizException("流程已发布，不能修改流程标识");
        }
        checkKeyUnique(model.getModelKey(), id);
        WfModel entity = new WfModel();
        entity.setId(id);
        copyBasic(model, entity);
        updateById(entity);
    }

    public void saveDesign(Long id, FlowDesign design) {
        getExisting(id);
        designValidator.validate(design);
        WfModel entity = new WfModel();
        entity.setId(id);
        entity.setDesignJson(objectMapper.writeValueAsString(design));
        updateById(entity);
    }

    /**
     * 发布：把当前设计转换为 BPMN，与设计 JSON 一起部署为新版本的流程定义。
     * 已发起的流程继续按原版本运行，之后发起的流程使用新版本
     *
     * @return 新版本号
     */
    @Transactional
    public int deploy(Long id) {
        WfModel model = getExisting(id);
        FlowDesign design = readDesign(model);
        designValidator.validate(design);
        BpmnModel bpmn = bpmnConverter.convert(model.getModelKey(), model.getName(), design.flow());
        Deployment deployment;
        try {
            deployment = repositoryService.createDeployment()
                    .name(model.getName())
                    .key(model.getModelKey())
                    .category(model.getCategory())
                    .tenantId(FlowTenant.current())
                    .addBpmnModel(model.getModelKey() + ".bpmn20.xml", bpmn)
                    .addString(FlowMetaService.DESIGN_RESOURCE, objectMapper.writeValueAsString(design))
                    .deploy();
        } catch (FlowableException e) {
            // 设计已通过校验，这里一般不会失败；引擎的异常信息可能包含内部细节，只记录日志
            log.error("流程发布失败，modelKey={}", model.getModelKey(), e);
            throw new BizException("流程发布失败，请检查流程设计");
        }
        ProcessDefinition definition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId()).singleResult();
        WfModel entity = new WfModel();
        entity.setId(id);
        entity.setVersion(definition.getVersion());
        entity.setDefinitionId(definition.getId());
        entity.setDeployTime(LocalDateTime.now());
        updateById(entity);
        return definition.getVersion();
    }

    public void changeStatus(Long id, Integer status) {
        getExisting(id);
        WfModel entity = new WfModel();
        entity.setId(id);
        entity.setStatus(status);
        updateById(entity);
    }

    /**
     * 删除模型。已发布的流程定义保留，运行中的流程不受影响，历史记录仍可查看
     */
    public void delete(List<Long> ids) {
        removeByIds(ids);
    }

    /** 可以发起的流程：已启用且已发布 */
    public List<WfModel> listStartable() {
        return list(Wrappers.<WfModel>lambdaQuery()
                .select(WfModel::getId, WfModel::getModelKey, WfModel::getName, WfModel::getCategory, WfModel::getIcon,
                        WfModel::getRemark)
                .eq(WfModel::getStatus, 1)
                .isNotNull(WfModel::getDefinitionId)
                .orderByAsc(WfModel::getSort).orderByAsc(WfModel::getId))
                .stream().filter(m -> business(m.getModelKey()) == null).toList();
    }

    /** 发起页面使用最新发布版本的表单与流程 */
    public StartFormView getStartForm(String modelKey) {
        WfModel model = getManualStartable(modelKey);
        FlowDesign design = metaService.get(model.getDefinitionId()).design();
        return new StartFormView(model.getDefinitionId(), model.getModelKey(), model.getName(), design.form(),
                design.flow());
    }

    /** 页面上可以发起的模型：业务单据的流程只能从业务单据提交 */
    public WfModel getManualStartable(String modelKey) {
        FlowBusiness b = business(modelKey);
        if (b != null) {
            throw new BizException("「{0}」请在对应的业务单据中提交审批", I18n.t(b.name()));
        }
        return getStartable(modelKey);
    }

    /**
     * 可以发起的模型
     *
     * @throws BizException 模型不存在、已停用或未发布
     */
    public WfModel getStartable(String modelKey) {
        WfModel model = getOne(Wrappers.<WfModel>lambdaQuery().eq(WfModel::getModelKey, modelKey));
        if (model == null || model.getDefinitionId() == null) {
            throw new BizException("流程不存在或未发布");
        }
        if (!Integer.valueOf(1).equals(model.getStatus())) {
            throw new BizException("流程「{0}」已停用", model.getName());
        }
        return model;
    }

    /** 模型存在、已启用且已发布 */
    public boolean isStartable(String modelKey) {
        return baseMapper.exists(Wrappers.<WfModel>lambdaQuery()
                .eq(WfModel::getModelKey, modelKey)
                .eq(WfModel::getStatus, 1)
                .isNotNull(WfModel::getDefinitionId));
    }

    /** 流程标识 → 分类，用于列表展示 */
    public Map<String, String> categoryMap(Collection<String> modelKeys) {
        if (modelKeys.isEmpty()) {
            return Map.of();
        }
        return list(Wrappers.<WfModel>lambdaQuery()
                        .select(WfModel::getModelKey, WfModel::getCategory)
                        .in(WfModel::getModelKey, modelKeys))
                .stream().filter(m -> m.getCategory() != null)
                .collect(Collectors.toMap(WfModel::getModelKey, WfModel::getCategory));
    }

    private WfModel getExisting(Long id) {
        WfModel model = getById(id);
        if (model == null) {
            throw new BizException("流程模型不存在");
        }
        return model;
    }

    private FlowDesign readDesign(WfModel model) {
        return model.getDesignJson() == null ? null : objectMapper.readValue(model.getDesignJson(), FlowDesign.class);
    }

    private void checkKeyUnique(String modelKey, Long id) {
        WfModel exist = getOne(Wrappers.<WfModel>lambdaQuery().eq(WfModel::getModelKey, modelKey));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("流程标识已存在");
        }
    }

    private static void copyBasic(WfModel from, WfModel to) {
        to.setModelKey(from.getModelKey());
        to.setName(from.getName());
        to.setCategory(from.getCategory());
        to.setIcon(from.getIcon());
        to.setSort(from.getSort() == null ? 0 : from.getSort());
        to.setRemark(from.getRemark());
    }
}
