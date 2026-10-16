package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.mes.entity.MesBomOp;
import com.lemon.erp.modules.mes.entity.MesEquipment;
import com.lemon.erp.modules.mes.entity.MesWorkCenter;
import com.lemon.erp.modules.mes.mapper.MesBomOpMapper;
import com.lemon.erp.modules.mes.mapper.MesEquipmentMapper;
import com.lemon.erp.modules.mes.mapper.MesWorkCenterMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MesWorkCenterService {

    private final MesWorkCenterMapper workCenterMapper;
    private final MesBomOpMapper bomOpMapper;
    private final MesEquipmentMapper equipmentMapper;

    public PageResult<MesWorkCenter> page(long current, long size, String keyword, Integer status) {
        Page<MesWorkCenter> page = workCenterMapper.selectPage(Page.of(current, size), Wrappers.<MesWorkCenter>lambdaQuery()
                .and(StringUtils.hasText(keyword), w -> w.like(MesWorkCenter::getCode, keyword).or().like(MesWorkCenter::getName, keyword))
                .eq(status != null, MesWorkCenter::getStatus, status)
                .orderByAsc(MesWorkCenter::getCode));
        return PageResult.of(page);
    }

    public MesWorkCenter get(Long id) {
        MesWorkCenter wc = id == null ? null : workCenterMapper.selectById(id);
        if (wc == null) {
            throw new BizException("工作中心不存在");
        }
        return wc;
    }

    /** 启用的工作中心 */
    public List<MesWorkCenter> options() {
        return workCenterMapper.selectList(Wrappers.<MesWorkCenter>lambdaQuery()
                .eq(MesWorkCenter::getStatus, 1).orderByAsc(MesWorkCenter::getCode));
    }

    public void create(MesWorkCenter wc) {
        checkCodeUnique(wc.getCode(), null);
        wc.setId(null);
        normalize(wc);
        if (wc.getStatus() == null) wc.setStatus(1);
        workCenterMapper.insert(wc);
    }

    public void update(Long id, MesWorkCenter wc) {
        get(id);
        checkCodeUnique(wc.getCode(), id);
        wc.setId(id);
        normalize(wc);
        wc.setStatus(null);
        workCenterMapper.updateById(wc);
    }

    public void changeStatus(Long id, Integer status) {
        get(id);
        MesWorkCenter entity = new MesWorkCenter();
        entity.setId(id);
        entity.setStatus(status);
        workCenterMapper.updateById(entity);
    }

    /** 被工艺路线或设备使用的不能删除，可以停用 */
    public void delete(List<Long> ids) {
        for (MesWorkCenter wc : workCenterMapper.selectByIds(ids)) {
            if (bomOpMapper.exists(Wrappers.<MesBomOp>lambdaQuery().eq(MesBomOp::getWorkCenterId, wc.getId()))
                    || equipmentMapper.exists(Wrappers.<MesEquipment>lambdaQuery().eq(MesEquipment::getWorkCenterId, wc.getId()))) {
                throw new BizException("工作中心「" + wc.getName() + "」已被工艺路线或设备使用，不能删除，可以停用");
            }
        }
        workCenterMapper.deleteByIds(ids);
    }

    private void checkCodeUnique(String code, Long id) {
        MesWorkCenter exist = workCenterMapper.selectOne(Wrappers.<MesWorkCenter>lambdaQuery().eq(MesWorkCenter::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("工作中心编码已存在");
        }
    }

    private static void normalize(MesWorkCenter wc) {
        if (wc.getDailyHours() == null) wc.setDailyHours(BigDecimal.valueOf(8));
        if (wc.getResources() == null) wc.setResources(1);
        if (wc.getWorkDays() == null || wc.getWorkDays().isBlank()) wc.setWorkDays("1,2,3,4,5");
    }
}
