package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.modules.mes.dto.BomItemRequest;
import com.lemon.erp.modules.mes.dto.BomOpRequest;
import com.lemon.erp.modules.mes.dto.BomRequest;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesBomItem;
import com.lemon.erp.modules.mes.entity.MesBomOp;
import com.lemon.erp.modules.mes.entity.MesEcn;
import com.lemon.erp.modules.mes.entity.MesWorkCenter;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesBomItemMapper;
import com.lemon.erp.modules.mes.mapper.MesBomMapper;
import com.lemon.erp.modules.mes.mapper.MesBomOpMapper;
import com.lemon.erp.modules.mes.mapper.MesEcnMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkCenterMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.EcnStatus;
import com.lemon.erp.modules.mes.model.RefDes;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 物料清单与工艺路线。一个产品可以有多个版本，其中一个为默认 BOM（MRP 展开、新建工单默认使用）。
 * 保存时校验循环引用：子件（含其下层）不能用到产品本身
 */
@Service
@RequiredArgsConstructor
public class MesBomService {

    private final MesBomMapper bomMapper;
    private final MesBomItemMapper itemMapper;
    private final MesBomOpMapper opMapper;
    private final MesWorkCenterMapper workCenterMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final WmsMaterialService materialService;
    private final MesEcnMapper ecnMapper;

    public PageResult<MesBom> page(long current, long size, String keyword, Integer status) {
        Page<MesBom> page = Page.of(current, size);
        bomMapper.selectPageList(page, keyword, status);
        return PageResult.of(page);
    }

    public MesBom detail(Long id) {
        MesBom bom = bomMapper.selectDetail(id);
        if (bom == null) {
            throw new BizException("mes.error.bomDoesNotExist");
        }
        bom.setItems(itemMapper.selectByBomId(id));
        bom.setOps(opMapper.selectByBomId(id));
        return bom;
    }

    /** 产品启用的 BOM，新建工单时选择 */
    public List<MesBom> listByMaterial(Long materialId) {
        return bomMapper.selectList(Wrappers.<MesBom>lambdaQuery()
                .eq(MesBom::getMaterialId, materialId)
                .eq(MesBom::getStatus, 1)
                .orderByDesc(MesBom::getIsDefault).orderByAsc(MesBom::getVersion));
    }

    @Transactional
    public Long create(BomRequest req) {
        validate(req);
        MesBom bom = new MesBom();
        fill(bom, req);
        bom.setStatus(1);
        try {
            bomMapper.insert(bom);
        } catch (DuplicateKeyException e) {
            throw new BizException("mes.error.productAlreadyHasBomVersion", req.version().trim());
        }
        saveLines(bom.getId(), req);
        afterSave(bom);
        return bom.getId();
    }

    /** 子件、工序整体替换；已下达的工单使用下达时的快照，不受影响 */
    @Transactional
    public void update(Long id, BomRequest req) {
        MesBom old = getBom(id);
        checkEcnEditable(old);
        validate(req);
        MesBom bom = new MesBom();
        fill(bom, req);
        bom.setId(id);
        boolean pending = isPending(old);
        if (pending) {
            // 工程变更中的新版本：产品不变，生效前保持停用、非默认
            bom.setMaterialId(old.getMaterialId());
            bom.setIsDefault(0);
        }
        try {
            bomMapper.updateById(bom);
        } catch (DuplicateKeyException e) {
            throw new BizException("mes.error.productAlreadyHasBomVersion", req.version().trim());
        }
        if (req.remark() == null && old.getRemark() != null) {
            bomMapper.update(Wrappers.<MesBom>lambdaUpdate().set(MesBom::getRemark, null).eq(MesBom::getId, id));
        }
        itemMapper.delete(Wrappers.<MesBomItem>lambdaQuery().eq(MesBomItem::getBomId, id));
        opMapper.delete(Wrappers.<MesBomOp>lambdaQuery().eq(MesBomOp::getBomId, id));
        saveLines(id, req);
        if (!pending) {
            bom.setMaterialId(req.materialId());
            afterSave(bom);
        }
    }

    public void changeStatus(Long id, Integer status) {
        checkNotPending(getBom(id));
        MesBom entity = new MesBom();
        entity.setId(id);
        entity.setStatus(status);
        bomMapper.updateById(entity);
    }

    /** 设为默认 BOM：同一产品的其他 BOM 取消默认 */
    @Transactional
    public void setDefault(Long id) {
        MesBom bom = getBom(id);
        checkNotPending(bom);
        bom.setIsDefault(1);
        bomMapper.update(Wrappers.<MesBom>lambdaUpdate().set(MesBom::getIsDefault, 1).eq(MesBom::getId, id));
        afterSave(bom);
    }

    /** 被工单使用的 BOM 不能删除，可以停用 */
    @Transactional
    public void delete(List<Long> ids) {
        for (Long id : ids) {
            if (workOrderMapper.exists(Wrappers.<MesWorkOrder>lambdaQuery().eq(MesWorkOrder::getBomId, id))) {
                throw new BizException("mes.error.bomUsedWorkOrdersCannotDeleted");
            }
            if (ecnMapper.exists(Wrappers.<MesEcn>lambdaQuery().and(w -> w.eq(MesEcn::getFromBomId, id).or().eq(MesEcn::getToBomId, id)))) {
                throw new BizException("mes.error.bomHasEcnRecordsCannotDeleted");
            }
        }
        itemMapper.delete(Wrappers.<MesBomItem>lambdaQuery().in(MesBomItem::getBomId, ids));
        opMapper.delete(Wrappers.<MesBomOp>lambdaQuery().in(MesBomOp::getBomId, ids));
        bomMapper.deleteByIds(ids);
    }

    // ---------------------------------------------------------------- 工程变更

    /** 复制基准 BOM 为新版本（停用、非默认），作为工程变更修改的对象 */
    @Transactional
    public Long copyForEcn(Long fromBomId, String version, Long ecnId) {
        MesBom from = getBom(fromBomId);
        MesBom bom = new MesBom();
        bom.setMaterialId(from.getMaterialId());
        bom.setVersion(version.trim());
        bom.setIsDefault(0);
        bom.setLeadDays(from.getLeadDays());
        bom.setRemark(from.getRemark());
        bom.setEcnId(ecnId);
        bom.setStatus(0);
        try {
            bomMapper.insert(bom);
        } catch (DuplicateKeyException e) {
            throw new BizException("mes.error.productAlreadyHasBomVersion", version.trim());
        }
        List<MesBomItem> items = itemMapper.selectByBomId(fromBomId);
        if (!items.isEmpty()) {
            itemMapper.insertBatch(items.stream().map(i -> {
                MesBomItem item = new MesBomItem();
                item.setBomId(bom.getId());
                item.setMaterialId(i.getMaterialId());
                item.setQty(i.getQty());
                item.setLossRate(i.getLossRate());
                item.setRefDes(i.getRefDes());
                item.setRemark(i.getRemark());
                return item;
            }).toList());
        }
        List<MesBomOp> ops = opMapper.selectByBomId(fromBomId);
        if (!ops.isEmpty()) {
            opMapper.insertBatch(ops.stream().map(o -> {
                MesBomOp op = new MesBomOp();
                op.setBomId(bom.getId());
                op.setSeq(o.getSeq());
                op.setName(o.getName());
                op.setWorkCenterId(o.getWorkCenterId());
                op.setStdHours(o.getStdHours());
                op.setPieceRate(o.getPieceRate());
                op.setFirstArticle(o.getFirstArticle());
                op.setRemark(o.getRemark());
                return op;
            }).toList());
        }
        return bom.getId();
    }

    /** 工程变更生效：新版本启用（原版本是默认时接替默认），原版本停用 */
    @Transactional
    public void activateForEcn(Long fromBomId, Long toBomId) {
        MesBom from = getBom(fromBomId);
        MesBom to = getBom(toBomId);
        bomMapper.update(Wrappers.<MesBom>lambdaUpdate().set(MesBom::getStatus, 1).eq(MesBom::getId, toBomId));
        if (Integer.valueOf(1).equals(from.getIsDefault())) {
            to.setIsDefault(1);
            bomMapper.update(Wrappers.<MesBom>lambdaUpdate().set(MesBom::getIsDefault, 1).eq(MesBom::getId, toBomId));
            afterSave(to);
        }
        bomMapper.update(Wrappers.<MesBom>lambdaUpdate().set(MesBom::getStatus, 0).eq(MesBom::getId, fromBomId));
    }

    /** 删除取消的工程变更创建的版本（没有被工单使用时） */
    @Transactional
    public void deleteForEcn(Long bomId) {
        if (workOrderMapper.exists(Wrappers.<MesWorkOrder>lambdaQuery().eq(MesWorkOrder::getBomId, bomId))) {
            return;
        }
        itemMapper.delete(Wrappers.<MesBomItem>lambdaQuery().eq(MesBomItem::getBomId, bomId));
        opMapper.delete(Wrappers.<MesBomOp>lambdaQuery().eq(MesBomOp::getBomId, bomId));
        bomMapper.deleteById(bomId);
    }

    /** 工程变更创建、还没有生效的版本 */
    private boolean isPending(MesBom bom) {
        if (bom.getEcnId() == null) {
            return false;
        }
        MesEcn ecn = ecnMapper.selectById(bom.getEcnId());
        return ecn != null && ecn.getStatus() != EcnStatus.EFFECTIVE;
    }

    private void checkEcnEditable(MesBom bom) {
        if (bom.getEcnId() == null) {
            return;
        }
        MesEcn ecn = ecnMapper.selectById(bom.getEcnId());
        if (ecn != null && ecn.getStatus() != EcnStatus.DRAFT && ecn.getStatus() != EcnStatus.EFFECTIVE) {
            throw new BizException("mes.error.ecnHasSubmittedNewBomNo", ecn.getEcnNo());
        }
    }

    private void checkNotPending(MesBom bom) {
        if (isPending(bom)) {
            throw new BizException("mes.error.versionCreatedEcnEnabledAutomaticallyOnce");
        }
    }

    // ---------------------------------------------------------------- 供 MRP、工单使用

    MesBom getBom(Long id) {
        MesBom bom = id == null ? null : bomMapper.selectById(id);
        if (bom == null) {
            throw new BizException("mes.error.bomDoesNotExist");
        }
        return bom;
    }

    /** 产品的默认 BOM（启用的），产品 ID → BOM；没有默认 BOM 的为采购件 */
    Map<Long, MesBom> defaultBoms(Collection<Long> materialIds) {
        if (materialIds.isEmpty()) {
            return Map.of();
        }
        return bomMapper.selectDefaults(materialIds).stream()
                .collect(Collectors.toMap(MesBom::getMaterialId, Function.identity(), (a, b) -> a));
    }

    /** BOM ID → 子件 */
    Map<Long, List<MesBomItem>> items(Collection<Long> bomIds) {
        if (bomIds.isEmpty()) {
            return Map.of();
        }
        return itemMapper.selectByBomIds(bomIds).stream().collect(Collectors.groupingBy(MesBomItem::getBomId));
    }

    List<MesBomItem> items(Long bomId) {
        return itemMapper.selectByBomId(bomId);
    }

    List<MesBomOp> ops(Long bomId) {
        return opMapper.selectByBomId(bomId);
    }

    // ---------------------------------------------------------------- 内部

    private void validate(BomRequest req) {
        WmsMaterial product = materialService.getEnabled(List.of(req.materialId())).get(req.materialId());
        Map<Long, WmsMaterial> components = materialService.getEnabled(req.items().stream().map(BomItemRequest::materialId).toList());
        Set<Long> seen = new HashSet<>();
        for (BomItemRequest item : req.items()) {
            WmsMaterial m = components.get(item.materialId());
            if (item.materialId().equals(req.materialId())) {
                throw new BizException("mes.error.componentCannotProductItself");
            }
            if (!seen.add(item.materialId())) {
                throw new BizException("mes.error.duplicateComponent", m.getCode(), m.getName());
            }
        }
        RefDes.checkUnique(req.items().stream().map(BomItemRequest::refDes).toList());
        List<BomOpRequest> ops = req.ops() == null ? List.of() : req.ops();
        if (!ops.isEmpty()) {
            Set<Long> wcIds = ops.stream().map(BomOpRequest::workCenterId).collect(Collectors.toSet());
            List<MesWorkCenter> wcs = workCenterMapper.selectByIds(wcIds);
            if (wcs.size() < wcIds.size()) {
                throw new BizException("mes.error.workCenterDoesNotExist");
            }
            if (ops.stream().map(BomOpRequest::seq).distinct().count() < ops.size()) {
                throw new BizException("mes.error.duplicateOperationNo");
            }
        }
        checkCycle(product, seen);
    }

    /** 从各子件出发沿已有 BOM 向下查找，到达产品本身即为循环引用 */
    private void checkCycle(WmsMaterial product, Set<Long> children) {
        Map<Long, List<Long>> edges = new HashMap<>();
        for (Map<String, Object> row : bomMapper.selectEdges()) {
            edges.computeIfAbsent(((Number) row.get("parent")).longValue(), k -> new ArrayList<>())
                    .add(((Number) row.get("child")).longValue());
        }
        Deque<Long> stack = new ArrayDeque<>(children);
        Set<Long> visited = new HashSet<>();
        while (!stack.isEmpty()) {
            Long m = stack.pop();
            if (m.equals(product.getId())) {
                throw new BizException("mes.error.circularBomReferenceProductUsedLower", product.getCode(), product.getName());
            }
            if (visited.add(m)) {
                stack.addAll(edges.getOrDefault(m, List.of()));
            }
        }
    }

    private static void fill(MesBom bom, BomRequest req) {
        bom.setMaterialId(req.materialId());
        bom.setVersion(req.version().trim());
        bom.setIsDefault(Integer.valueOf(1).equals(req.isDefault()) ? 1 : 0);
        bom.setLeadDays(req.leadDays() == null ? 0 : req.leadDays());
        bom.setRemark(req.remark());
    }

    private void saveLines(Long bomId, BomRequest req) {
        itemMapper.insertBatch(req.items().stream().map(r -> {
            MesBomItem item = new MesBomItem();
            item.setBomId(bomId);
            item.setMaterialId(r.materialId());
            item.setQty(r.qty());
            item.setLossRate(Decimals.nz(r.lossRate()));
            item.setRefDes(RefDes.normalize(r.refDes()));
            item.setRemark(r.remark());
            return item;
        }).toList());
        if (req.ops() != null && !req.ops().isEmpty()) {
            opMapper.insertBatch(req.ops().stream().map(r -> {
                MesBomOp op = new MesBomOp();
                op.setBomId(bomId);
                op.setSeq(r.seq());
                op.setName(r.name().trim());
                op.setWorkCenterId(r.workCenterId());
                op.setStdHours(Decimals.nz(r.stdHours()));
                op.setPieceRate(r.pieceRate());
                op.setFirstArticle(Boolean.TRUE.equals(r.firstArticle()) ? 1 : 0);
                op.setRemark(r.remark());
                return op;
            }).toList());
        }
    }

    /** 产品第一个 BOM 自动设为默认；设为默认时取消同一产品其他 BOM 的默认 */
    private void afterSave(MesBom bom) {
        if (Integer.valueOf(1).equals(bom.getIsDefault())) {
            bomMapper.update(Wrappers.<MesBom>lambdaUpdate()
                    .set(MesBom::getIsDefault, 0)
                    .eq(MesBom::getMaterialId, bom.getMaterialId())
                    .ne(MesBom::getId, bom.getId()));
        } else if (!bomMapper.exists(Wrappers.<MesBom>lambdaQuery()
                .eq(MesBom::getMaterialId, bom.getMaterialId()).eq(MesBom::getIsDefault, 1))) {
            bomMapper.update(Wrappers.<MesBom>lambdaUpdate().set(MesBom::getIsDefault, 1).eq(MesBom::getId, bom.getId()));
        }
    }
}
