package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.common.util.Decimals;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.message.MessageCategory;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.dto.MoldRequests;
import com.lemon.erp.modules.mes.entity.MesEquipment;
import com.lemon.erp.modules.mes.entity.MesMold;
import com.lemon.erp.modules.mes.entity.MesMoldLog;
import com.lemon.erp.modules.mes.entity.MesWorkOrder;
import com.lemon.erp.modules.mes.mapper.MesEquipmentMapper;
import com.lemon.erp.modules.mes.mapper.MesMoldLogMapper;
import com.lemon.erp.modules.mes.mapper.MesMoldMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkOrderMapper;
import com.lemon.erp.modules.mes.model.EquipmentStatus;
import com.lemon.erp.modules.mes.model.MoldAction;
import com.lemon.erp.modules.mes.model.MoldStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模具工装：台账（适用产品、模穴数、设计寿命），领用到工单 / 归还、送修 / 修复、报废；
 * 报工时按 ⌈产出 / 模穴数⌉ 累计模次，达到寿命预警比例、设计寿命时提醒模具管理人员（mes:mold:edit），达到寿命的不能再领用
 */
@Service
@RequiredArgsConstructor
public class MesMoldService {

    private final MesMoldMapper moldMapper;
    private final MesMoldLogMapper logMapper;
    private final MesWorkOrderMapper workOrderMapper;
    private final MesEquipmentMapper equipmentMapper;
    private final MessageSender messageSender;
    private final DictLookup dictLookup;

    public PageResult<MesMold> page(long current, long size, String keyword, String moldType, MoldStatus status, Long productId,
                                    Boolean warn) {
        Page<MesMold> page = Page.of(current, size);
        moldMapper.selectPageList(page, keyword, moldType, status, productId, warn);
        fillProducts(page.getRecords());
        return PageResult.of(page);
    }

    public MesMold detail(Long id) {
        MesMold mold = id == null ? null : moldMapper.selectDetail(id);
        if (mold == null) {
            throw new BizException("mes.error.moldDoesNotExist");
        }
        fillProducts(List.of(mold));
        return mold;
    }

    /** 领用到工单、可在报工中选择的模具 */
    public List<MesMold> byWorkOrder(Long woId) {
        return moldMapper.selectByWo(woId);
    }

    public List<MesMoldLog> logs(Long id) {
        detail(id);
        return moldMapper.selectLogs(id);
    }

    @Transactional
    public Long create(MoldRequests.Save req) {
        checkCode(req.code(), null);
        MesMold mold = new MesMold();
        fill(mold, req);
        mold.setUsedCount(req.usedCount() == null ? 0 : req.usedCount());
        mold.setStatus(MoldStatus.AVAILABLE);
        moldMapper.insert(mold);
        saveProducts(mold.getId(), req.productIds());
        return mold.getId();
    }

    @Transactional
    public void update(Long id, MoldRequests.Save req) {
        MesMold mold = detail(id);
        checkCode(req.code(), id);
        fill(mold, req);
        moldMapper.update(Wrappers.<MesMold>lambdaUpdate()
                .set(MesMold::getCode, mold.getCode()).set(MesMold::getName, mold.getName())
                .set(MesMold::getMoldType, mold.getMoldType()).set(MesMold::getSpec, mold.getSpec())
                .set(MesMold::getCavities, mold.getCavities()).set(MesMold::getDesignLife, mold.getDesignLife())
                .set(MesMold::getWarnRatio, mold.getWarnRatio()).set(MesMold::getLocation, mold.getLocation())
                .set(MesMold::getRemark, mold.getRemark()).set(MesMold::getUpdateTime, LocalDateTime.now())
                .eq(MesMold::getId, id));
        moldMapper.deleteProducts(id);
        saveProducts(id, req.productIds());
    }

    /** 有使用记录的不能删除，可以报废 */
    @Transactional
    public void delete(Long id) {
        MesMold mold = detail(id);
        if (mold.getStatus() == MoldStatus.IN_USE
                || logMapper.exists(Wrappers.<MesMoldLog>lambdaQuery().eq(MesMoldLog::getMoldId, id).eq(MesMoldLog::getAction, MoldAction.USE))) {
            throw new BizException("mes.error.moldHasUsageRecordsUseCannot", mold.getCode());
        }
        moldMapper.deleteProducts(id);
        logMapper.delete(Wrappers.<MesMoldLog>lambdaQuery().eq(MesMoldLog::getMoldId, id));
        moldMapper.deleteById(id);
    }

    /** 领用到生产中的工单：模具须在库、适用于工单产品、未达到设计寿命 */
    @Transactional
    public void issue(Long id, MoldRequests.Action req) {
        MesMold mold = detail(id);
        if (req.woId() == null) {
            throw new BizException("mes.valid.selectWorkOrder");
        }
        MesWorkOrder wo = workOrderMapper.selectById(req.woId());
        if (wo == null || !wo.getStatus().isExecutable()) {
            throw new BizException("mes.error.moldsOnlyCheckedOutReleasedProduction");
        }
        if (mold.getProductIds() != null && !mold.getProductIds().isEmpty() && !mold.getProductIds().contains(wo.getMaterialId())) {
            throw new BizException("mes.error.moldNotApplicableProductWorkOrder", mold.getCode(), wo.getWoNo());
        }
        if (mold.getDesignLife() != null && mold.getUsedCount() >= mold.getDesignLife()) {
            throw new BizException("mes.error.moldHasReachedDesignLifeShots", mold.getCode(), mold.getDesignLife());
        }
        if (req.equipmentId() != null) {
            MesEquipment e = equipmentMapper.selectById(req.equipmentId());
            if (e == null || e.getStatus() == EquipmentStatus.SCRAPPED) {
                throw new BizException("mes.error.equipmentDoesNotExistHasScrapped");
            }
        }
        transit(id, MoldStatus.AVAILABLE, MoldStatus.IN_USE, req.woId(), req.equipmentId(), null, "mes.label.stock");
        log(id, MoldAction.ISSUE, req.date(), req.woId(), req.equipmentId(), null, null, req.remark());
    }

    @Transactional
    public void giveBack(Long id, MoldRequests.Action req) {
        MesMold mold = detail(id);
        transit(id, MoldStatus.IN_USE, MoldStatus.AVAILABLE, null, null, req.location(), "mes.label.use");
        log(id, MoldAction.RETURN, req.date(), mold.getWoId(), mold.getEquipmentId(), null, null, req.remark());
    }

    @Transactional
    public void repair(Long id, MoldRequests.Action req) {
        detail(id);
        transit(id, MoldStatus.AVAILABLE, MoldStatus.REPAIR, null, null, null, "mes.label.stockReturnMoldsUseFirst");
        log(id, MoldAction.REPAIR, req.date(), null, null, null, null, req.remark());
    }

    @Transactional
    public void repaired(Long id, MoldRequests.Action req) {
        detail(id);
        transit(id, MoldStatus.REPAIR, MoldStatus.AVAILABLE, null, null, req.location(), "mes.label.underRepair");
        log(id, MoldAction.REPAIRED, req.date(), null, null, null, null, req.remark());
    }

    @Transactional
    public void scrap(Long id, MoldRequests.Action req) {
        MesMold mold = detail(id);
        if (mold.getStatus() == MoldStatus.IN_USE || mold.getStatus() == MoldStatus.SCRAPPED) {
            if (mold.getStatus() == MoldStatus.IN_USE) {
                throw new BizException("mes.error.moldUseReturnFirst");
            }
            throw new BizException("mes.error.moldHasScrapped");
        }
        transit(id, mold.getStatus(), MoldStatus.SCRAPPED, null, null, null, "mes.label.stockUnderRepair");
        log(id, MoldAction.SCRAP, req.date(), null, null, null, null, req.remark());
    }

    /**
     * 报工使用模具：模具须领用到该工单；模次 = ⌈(合格 + 报废) / 模穴数⌉
     *
     * @return 模次
     */
    @Transactional
    public int use(Long moldId, Long woId, Long equipmentId, Long reportId, LocalDate date, BigDecimal qty) {
        MesMold mold = detail(moldId);
        if (mold.getStatus() != MoldStatus.IN_USE || !woId.equals(mold.getWoId())) {
            throw new BizException("mes.error.moldNotCheckedOutWorkOrder", mold.getCode());
        }
        int shots = qty.divide(BigDecimal.valueOf(mold.getCavities()), 0, RoundingMode.CEILING).intValueExact();
        if (shots <= 0) {
            return 0;
        }
        moldMapper.addUsed(moldId, shots);
        log(moldId, MoldAction.USE, date, woId, equipmentId != null ? equipmentId : mold.getEquipmentId(), reportId, shots, null);
        Integer life = mold.getDesignLife();
        if (life != null) {
            int before = mold.getUsedCount();
            int after = before + shots;
            BigDecimal warnAt = BigDecimal.valueOf(life).multiply(mold.getWarnRatio()).divide(Decimals.HUNDRED, 0, RoundingMode.CEILING);
            String name = "「" + mold.getCode() + " " + mold.getName() + "」";
            if (after >= life && before < life) {
                messageSender.sendToPermission("mes:mold:edit", MessageSender.Message.of(MessageCategory.ALERT, "模具" + name + "已达到设计寿命", // i18n-ignore
                        "累计 " + after + " / " + life + " 模次，归还后不能再领用，请安排维修或报废", "/mes/mold").dedup("mold-life:" + moldId + ":" + life)); // i18n-ignore
            } else if (after < life && BigDecimal.valueOf(after).compareTo(warnAt) >= 0 && BigDecimal.valueOf(before).compareTo(warnAt) < 0) {
                messageSender.sendToPermission("mes:mold:edit", MessageSender.Message.of(MessageCategory.REMIND, "模具" + name + "即将到达寿命", // i18n-ignore
                        "累计 " + after + " / " + life + " 模次", "/mes/mold").dedup("mold-warn:" + moldId + ":" + life)); // i18n-ignore
            }
        }
        return shots;
    }

    private void transit(Long id, MoldStatus from, MoldStatus to, Long woId, Long equipmentId, String location, String fromTextKey) {
        if (!StatusTransitions.tryChange(moldMapper, MesMold::getId, id, MesMold::getStatus, from, to,
                w -> w.set(MesMold::getWoId, woId).set(MesMold::getEquipmentId, equipmentId)
                        .set(StringUtils.hasText(location), MesMold::getLocation, StringUtils.hasText(location) ? location.trim() : null)
                        .set(MesMold::getUpdateTime, LocalDateTime.now()))) {
            throw new BizException("mes.error.onlyMoldsOperated", Messages.get(fromTextKey));
        }
    }

    private void log(Long moldId, MoldAction action, LocalDate date, Long woId, Long equipmentId, Long reportId, Integer shots, String remark) {
        MesMoldLog l = new MesMoldLog();
        l.setMoldId(moldId);
        l.setAction(action);
        l.setLogDate(date == null ? LocalDate.now() : date);
        l.setWoId(woId);
        l.setEquipmentId(equipmentId);
        l.setReportId(reportId);
        l.setShots(shots);
        l.setRemark(remark);
        l.setCreateBy(SecurityUtils.getUserId());
        l.setCreateTime(LocalDateTime.now());
        logMapper.insert(l);
    }

    private void fill(MesMold mold, MoldRequests.Save req) {
        dictLookup.check("mes_mold_type", req.moldType(), "mes.label.moldType");
        mold.setCode(req.code().trim());
        mold.setName(req.name().trim());
        mold.setMoldType(req.moldType());
        mold.setSpec(req.spec());
        mold.setCavities(req.cavities());
        mold.setDesignLife(req.designLife());
        mold.setWarnRatio(req.warnRatio());
        mold.setLocation(StringUtils.hasText(req.location()) ? req.location().trim() : null);
        mold.setRemark(req.remark());
    }

    private void saveProducts(Long moldId, List<Long> productIds) {
        if (productIds != null && !productIds.isEmpty()) {
            moldMapper.insertProducts(moldId, productIds.stream().distinct().toList());
        }
    }

    private void checkCode(String code, Long id) {
        if (moldMapper.exists(Wrappers.<MesMold>lambdaQuery().eq(MesMold::getCode, code.trim()).ne(id != null, MesMold::getId, id))) {
            throw new BizException("mes.error.codeAlreadyExists", code);
        }
    }

    private void fillProducts(List<MesMold> molds) {
        if (molds.isEmpty()) {
            return;
        }
        Set<Long> ids = molds.stream().map(MesMold::getId).collect(Collectors.toSet());
        Map<Long, List<Map<String, Object>>> byMold = new HashMap<>();
        moldMapper.selectProducts(ids).forEach(p -> byMold.computeIfAbsent(((Number) p.get("moldId")).longValue(), k -> new ArrayList<>()).add(p));
        for (MesMold m : molds) {
            List<Map<String, Object>> list = byMold.getOrDefault(m.getId(), List.of());
            m.setProductIds(list.stream().map(p -> ((Number) p.get("materialId")).longValue()).toList());
            m.setProductNames(list.stream().map(p -> p.get("code") + " " + p.get("name")).collect(Collectors.joining("、")));
        }
    }
}
