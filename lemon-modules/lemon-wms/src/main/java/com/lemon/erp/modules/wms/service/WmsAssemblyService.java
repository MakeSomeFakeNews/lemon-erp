package com.lemon.erp.modules.wms.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.LoginUser;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.framework.security.datascope.DataPermission;
import com.lemon.erp.framework.security.datascope.DataPermissionContext;
import com.lemon.erp.framework.security.datascope.DataPermissionRule;
import com.lemon.erp.modules.wms.dto.AssemblyRequest;
import com.lemon.erp.modules.wms.dto.DocQuery;
import com.lemon.erp.modules.wms.dto.StockDocItemRequest;
import com.lemon.erp.modules.wms.dto.StockDocRequest;
import com.lemon.erp.modules.wms.entity.WmsAssembly;
import com.lemon.erp.modules.wms.entity.WmsAssemblyItem;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.entity.WmsStockDoc;
import com.lemon.erp.modules.wms.mapper.WmsAssemblyItemMapper;
import com.lemon.erp.modules.wms.mapper.WmsAssemblyMapper;
import com.lemon.erp.modules.wms.mapper.WmsMaterialMapper;
import com.lemon.erp.modules.wms.model.AssemblyKind;
import com.lemon.erp.modules.wms.model.DocStatus;
import com.lemon.erp.modules.wms.model.StockBizType;
import com.lemon.erp.modules.wms.model.StockInType;
import com.lemon.erp.modules.wms.model.StockOutType;
import com.lemon.erp.modules.wms.model.WmsConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组装拆卸单：组装消耗子件得到成套件，拆卸把成套件拆为子件（如礼盒装、整箱拆零）。草稿可以修改、删除；
 * 确认后在同一事务中生成并确认出库单、入库单（出入库类型 ASSEMBLY，来源单号为本单号），之后不能修改，需要撤回时做一张反向的单据。
 * 成本核算时组装的成套件按所耗子件的成本入账，拆卸的子件按成套件成本分摊入账
 */
@Service
@RequiredArgsConstructor
public class WmsAssemblyService {

    /** 单据编号规则标识 */
    public static final String DOC_NO = "ZZ";
    static final DataPermissionRule RULE = new DataPermissionRule("wms_assembly", "dept_id", "create_by");

    private final WmsAssemblyMapper assemblyMapper;
    private final WmsAssemblyItemMapper itemMapper;
    private final WmsMaterialMapper materialMapper;
    private final WmsStockDocService docService;
    private final DocNoService docNoService;

    @DataPermission(table = "wms_assembly", deptColumn = "dept_id", userColumn = "create_by")
    public PageResult<WmsAssembly> page(long current, long size, DocQuery query) {
        Page<WmsAssembly> page = Page.of(current, size);
        assemblyMapper.selectPageList(page, query);
        return PageResult.of(page);
    }

    public WmsAssembly detail(Long id) {
        getAccessible(id);
        WmsAssembly a = assemblyMapper.selectDetail(id);
        a.setItems(itemMapper.selectByAssemblyId(id));
        return a;
    }

    @Transactional
    public Long create(AssemblyRequest req) {
        validate(req);
        LoginUser user = SecurityUtils.getLoginUser();
        WmsAssembly a = new WmsAssembly();
        fill(a, req);
        a.setAssemblyNo(docNoService.next(DOC_NO));
        a.setStatus(DocStatus.DRAFT);
        a.setDeptId(user.deptId());
        a.setCreateBy(user.userId());
        assemblyMapper.insert(a);
        saveItems(a.getId(), req.items());
        return a.getId();
    }

    @Transactional
    public void update(Long id, AssemblyRequest req) {
        WmsAssembly old = getDraft(id);
        validate(req);
        fill(old, req);
        old.setUpdateTime(LocalDateTime.now());
        assemblyMapper.updateById(old);
        // 可为空的字段单独更新
        assemblyMapper.update(Wrappers.<WmsAssembly>lambdaUpdate()
                .set(WmsAssembly::getProductionDate, req.productionDate())
                .set(WmsAssembly::getExpiryDate, req.expiryDate())
                .set(WmsAssembly::getSerialNos, old.getSerialNos())
                .set(WmsAssembly::getRemark, req.remark())
                .eq(WmsAssembly::getId, id));
        itemMapper.delete(Wrappers.<WmsAssemblyItem>lambdaQuery().eq(WmsAssemblyItem::getAssemblyId, id));
        saveItems(id, req.items());
    }

    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            getDraft(id);
        }
        itemMapper.delete(Wrappers.<WmsAssemblyItem>lambdaQuery().in(WmsAssemblyItem::getAssemblyId, ids));
        assemblyMapper.deleteByIds(ids);
    }

    /**
     * 确认：组装 = 子件出库 + 成套件入库，拆卸 = 成套件出库 + 子件入库；库存不足、序列号不在库等错误整单回滚
     */
    @Transactional
    public void confirm(Long id) {
        WmsAssembly a = getDraft(id);
        List<WmsAssemblyItem> items = itemMapper.selectList(Wrappers.<WmsAssemblyItem>lambdaQuery()
                .eq(WmsAssemblyItem::getAssemblyId, id).orderByAsc(WmsAssemblyItem::getId));
        if (assemblyMapper.update(Wrappers.<WmsAssembly>lambdaUpdate()
                .set(WmsAssembly::getStatus, DocStatus.DONE)
                .set(WmsAssembly::getConfirmBy, SecurityUtils.getUserId())
                .set(WmsAssembly::getConfirmTime, LocalDateTime.now())
                .eq(WmsAssembly::getId, id).eq(WmsAssembly::getStatus, DocStatus.DRAFT)) == 0) {
            throw new BizException("wms.error.documentHasConfirmedRefresh");
        }
        List<StockDocItemRequest> parts = items.stream().map(i -> new StockDocItemRequest(i.getMaterialId(), i.getLocationId(), null,
                i.getBatchNo(), i.getProductionDate(), i.getExpiryDate(), i.getQty(), i.getSerialNos(), i.getRemark())).toList();
        List<StockDocItemRequest> product = List.of(new StockDocItemRequest(a.getMaterialId(), a.getLocationId(), null, a.getBatchNo(),
                a.getProductionDate(), a.getExpiryDate(), a.getQty(), a.getSerialNos(), null));
        boolean assemble = a.getKind() == AssemblyKind.ASSEMBLE;
        String remark = (assemble ? "组装 " : "拆卸 ") + a.getAssemblyNo(); // i18n-ignore 存入单据备注
        // 先出库后入库：组装时子件先出库，拆卸时成套件先出库
        WmsStockDoc out = docService.createConfirmed(StockBizType.OUT, new StockDocRequest(StockOutType.ASSEMBLY, a.getWarehouseId(), null,
                a.getBizDate(), remark, assemble ? parts : product), a.getAssemblyNo());
        WmsStockDoc in = docService.createConfirmed(StockBizType.IN, new StockDocRequest(StockInType.ASSEMBLY, a.getWarehouseId(), null,
                a.getBizDate(), remark, assemble ? product : parts), a.getAssemblyNo());
        assemblyMapper.update(Wrappers.<WmsAssembly>lambdaUpdate()
                .set(WmsAssembly::getOutDocId, out.getId()).set(WmsAssembly::getOutDocNo, out.getDocNo())
                .set(WmsAssembly::getInDocId, in.getId()).set(WmsAssembly::getInDocNo, in.getDocNo())
                .eq(WmsAssembly::getId, id));
    }

    // ---------------------------------------------------------------- 内部

    private void validate(AssemblyRequest req) {
        Map<Long, WmsMaterial> materials = materialMapper.selectByIds(
                java.util.stream.Stream.concat(java.util.stream.Stream.of(req.materialId()), req.items().stream().map(StockDocItemRequest::materialId))
                        .distinct().toList()).stream().collect(Collectors.toMap(WmsMaterial::getId, Function.identity()));
        WmsMaterial product = materials.get(req.materialId());
        if (product == null) {
            throw new BizException("wms.error.kitDoesNotExist");
        }
        for (StockDocItemRequest i : req.items()) {
            if (i.materialId().equals(req.materialId())) {
                throw new BizException("wms.error.componentCannotSameKit");
            }
            if (!materials.containsKey(i.materialId())) {
                throw new BizException("wms.error.componentDoesNotExist");
            }
        }
        materials.values().stream().filter(WmsMaterial::isTemplateMaterial).findFirst().ifPresent(m -> {
            throw new BizException("wms.error.multiVariantStyleSelectSpecificVariant2", m.getCode());
        });
    }

    private static void fill(WmsAssembly a, AssemblyRequest req) {
        a.setKind(req.kind());
        a.setWarehouseId(req.warehouseId());
        a.setBizDate(req.bizDate());
        a.setMaterialId(req.materialId());
        a.setLocationId(WmsConstants.locationOrNone(req.locationId()));
        a.setBatchNo(WmsConstants.batchOrNone(req.batchNo()));
        a.setProductionDate(req.productionDate());
        a.setExpiryDate(req.expiryDate());
        List<String> serials = WmsConstants.splitSerials(req.serialNos());
        a.setSerialNos(serials.isEmpty() ? null : String.join(",", serials));
        a.setQty(req.qty());
        a.setRemark(req.remark());
    }

    private void saveItems(Long assemblyId, List<StockDocItemRequest> items) {
        for (StockDocItemRequest r : items) {
            WmsAssemblyItem item = new WmsAssemblyItem();
            item.setAssemblyId(assemblyId);
            item.setMaterialId(r.materialId());
            item.setLocationId(WmsConstants.locationOrNone(r.locationId()));
            item.setBatchNo(WmsConstants.batchOrNone(r.batchNo()));
            item.setProductionDate(r.productionDate());
            item.setExpiryDate(r.expiryDate());
            item.setQty(r.qty());
            List<String> serials = WmsConstants.splitSerials(r.serialNos());
            item.setSerialNos(serials.isEmpty() ? null : String.join(",", serials));
            item.setRemark(r.remark());
            itemMapper.insert(item);
        }
    }

    /** 按数据权限读取 */
    private WmsAssembly getAccessible(Long id) {
        WmsAssembly a = DataPermissionContext.execute(RULE, () -> assemblyMapper.selectById(id));
        if (a == null) {
            throw new BizException("wms.error.assemblyOrderDoesNotExist");
        }
        return a;
    }

    private WmsAssembly getDraft(Long id) {
        WmsAssembly a = getAccessible(id);
        if (a.getStatus() != DocStatus.DRAFT) {
            throw new BizException("wms.error.hasConfirmedCannotModified", a.getAssemblyNo());
        }
        return a;
    }
}
