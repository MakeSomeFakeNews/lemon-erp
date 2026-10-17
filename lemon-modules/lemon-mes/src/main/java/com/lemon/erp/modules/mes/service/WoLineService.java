package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.modules.mes.entity.MesBom;
import com.lemon.erp.modules.mes.entity.MesBomItem;
import com.lemon.erp.modules.mes.entity.MesMaterialPlan;
import com.lemon.erp.modules.mes.entity.MesWoMaterial;
import com.lemon.erp.modules.mes.entity.MesWoOp;
import com.lemon.erp.modules.mes.mapper.MesWoMaterialMapper;
import com.lemon.erp.modules.mes.mapper.MesWoOpMapper;
import com.lemon.erp.modules.mes.model.SupplyType;
import com.lemon.erp.modules.wms.entity.WmsMaterial;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工单的用料和工序行：按 BOM 生成（虚拟件展开见 {@link WoBomExpander}）、改量时重算需求数量、删除。
 * 不单独开事务，由调用方（{@link MesWorkOrderService}、{@link WoChangeService}）的事务包含
 */
@Service
@RequiredArgsConstructor
class WoLineService {

    private final MesWoMaterialMapper woMaterialMapper;
    private final MesWoOpMapper woOpMapper;
    private final MesBomService bomService;
    private final MesMaterialService materialPlanService;
    private final WmsMaterialService materialService;

    List<MesWoMaterial> materials(Long woId) {
        return woMaterialMapper.selectByWo(woId);
    }

    List<MesWoOp> ops(Long woId) {
        return woOpMapper.selectByWo(woId);
    }

    /** 按 BOM 生成工单用料（虚拟件展开为它的子件，同一物料合并；序列号物料向上取整）和工序 */
    void generate(Long woId, MesBom bom, BigDecimal qty) {
        Map<Long, BigDecimal> unitQtys = WoBomExpander.expand(bomService.items(bom.getId()), phantomSource());
        Map<Long, WmsMaterial> materials = materialService.mapByIds(unitQtys.keySet());
        woMaterialMapper.insertBatch(unitQtys.entrySet().stream().map(e -> {
            MesWoMaterial m = new MesWoMaterial();
            m.setWoId(woId);
            m.setMaterialId(e.getKey());
            m.setUnitQty(WoBomExpander.roundUnitQty(e.getValue()));
            m.setRequiredQty(WoBomExpander.requiredQty(qty, m.getUnitQty(), materials.get(e.getKey()).isSerialManaged()));
            return m;
        }).toList());
        insertOps(woId, bom);
    }

    /** 返工工单：用料为产品本身，单位用量 1 */
    void generateRework(Long woId, Long materialId, BigDecimal qty, MesBom bom) {
        MesWoMaterial self = new MesWoMaterial();
        self.setWoId(woId);
        self.setMaterialId(materialId);
        self.setUnitQty(BigDecimal.ONE.setScale(6));
        self.setRequiredQty(qty);
        woMaterialMapper.insertBatch(List.of(self));
        insertOps(woId, bom);
    }

    /** 删除工单的用料和工序 */
    void delete(Collection<Long> woIds) {
        woMaterialMapper.delete(Wrappers.<MesWoMaterial>lambdaQuery().in(MesWoMaterial::getWoId, woIds));
        woOpMapper.delete(Wrappers.<MesWoOp>lambdaQuery().in(MesWoOp::getWoId, woIds));
    }

    /** 工单改量后按新数量重算用料需求 */
    void recalcRequired(Long woId, BigDecimal qty) {
        Map<Long, WmsMaterial> materials = materialService.mapByIds(woMaterialMapper.selectByWo(woId).stream().map(MesWoMaterial::getMaterialId).toList());
        for (MesWoMaterial m : woMaterialMapper.selectByWo(woId)) {
            boolean whole = materials.get(m.getMaterialId()).isSerialManaged();
            woMaterialMapper.update(Wrappers.<MesWoMaterial>lambdaUpdate()
                    .set(MesWoMaterial::getRequiredQty, WoBomExpander.requiredQty(qty, m.getUnitQty(), whole))
                    .eq(MesWoMaterial::getId, m.getId()));
        }
    }

    private void insertOps(Long woId, MesBom bom) {
        List<MesWoOp> ops = bomService.ops(bom.getId()).stream().map(o -> {
            MesWoOp op = new MesWoOp();
            op.setWoId(woId);
            op.setSeq(o.getSeq());
            op.setName(o.getName());
            op.setWorkCenterId(o.getWorkCenterId());
            op.setStdHours(o.getStdHours());
            op.setPieceRate(o.getPieceRate());
            op.setFirstArticle(o.getFirstArticle());
            return op;
        }).toList();
        if (!ops.isEmpty()) {
            woOpMapper.insertBatch(ops);
        }
    }

    /** 虚拟件：物料计划属性设为虚拟件且有默认 BOM */
    private WoBomExpander.BomSource phantomSource() {
        return new WoBomExpander.BomSource() {
            @Override
            public Map<Long, Long> phantomBoms(List<Long> materialIds) {
                Map<Long, MesMaterialPlan> plans = materialPlanService.plans(materialIds);
                List<Long> phantomIds = materialIds.stream()
                        .filter(id -> plans.containsKey(id) && plans.get(id).getSupplyType() == SupplyType.PHANTOM).toList();
                Map<Long, Long> result = new HashMap<>();
                bomService.defaultBoms(phantomIds).forEach((id, bom) -> result.put(id, bom.getId()));
                return result;
            }

            @Override
            public List<MesBomItem> items(Long bomId) {
                return bomService.items(bomId);
            }
        };
    }
}
