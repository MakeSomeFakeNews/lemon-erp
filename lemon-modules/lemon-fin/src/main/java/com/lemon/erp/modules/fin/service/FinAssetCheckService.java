package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.fin.dto.AssetCheckRequests;
import com.lemon.erp.modules.fin.dto.AssetMoveRequest;
import com.lemon.erp.modules.fin.dto.DisposeRequest;
import com.lemon.erp.modules.fin.entity.FinAsset;
import com.lemon.erp.modules.fin.entity.FinAssetCheck;
import com.lemon.erp.modules.fin.entity.FinAssetCheckItem;
import com.lemon.erp.modules.fin.mapper.FinAssetCheckItemMapper;
import com.lemon.erp.modules.fin.mapper.FinAssetCheckMapper;
import com.lemon.erp.modules.fin.model.AssetCheckResult;
import com.lemon.erp.modules.fin.model.AssetCheckStatus;
import com.lemon.erp.modules.fin.model.AssetMoveType;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 资产盘点：按部门 / 存放地点生成盘点清单（账面部门、使用人、地点快照），逐项录入相符、位置变化、盘亏，可登记盘盈；
 * 完成时位置变化的更新卡片（记入变动履历），盘亏的按盘亏处置（生成处置凭证，当月须先计提折旧）
 */
@Service
@RequiredArgsConstructor
public class FinAssetCheckService {

    /** 资产盘点单号的编码规则标识 */
    public static final String DOC_NO = "ZCPD";

    private final FinAssetCheckMapper checkMapper;
    private final FinAssetCheckItemMapper itemMapper;
    private final FinAssetService assetService;
    private final DocNoService docNoService;

    public PageResult<FinAssetCheck> page(long current, long size, AssetCheckStatus status) {
        Page<FinAssetCheck> page = Page.of(current, size);
        checkMapper.selectPageList(page, status);
        return PageResult.of(page);
    }

    public FinAssetCheck detail(Long id) {
        FinAssetCheck check = id == null ? null : checkMapper.selectDetail(id);
        if (check == null) {
            throw new BizException("盘点单不存在");
        }
        check.setItems(checkMapper.selectItems(id));
        return check;
    }

    @Transactional
    public Long create(AssetCheckRequests.Create req) {
        String location = StringUtils.hasText(req.location()) ? req.location().trim() : null;
        List<FinAsset> assets = checkMapper.selectScope(req.deptId(), location);
        if (assets.isEmpty()) {
            throw new BizException("盘点范围内没有资产");
        }
        FinAssetCheck check = new FinAssetCheck();
        check.setCheckNo(docNoService.next(DOC_NO));
        check.setCheckDate(req.checkDate());
        check.setDeptId(req.deptId());
        check.setLocation(location);
        check.setStatus(AssetCheckStatus.CHECKING);
        check.setRemark(req.remark());
        check.setCreateBy(SecurityUtils.getUserId());
        checkMapper.insert(check);
        checkMapper.insertItems(assets.stream().map(a -> {
            FinAssetCheckItem item = new FinAssetCheckItem();
            item.setCheckId(check.getId());
            item.setAssetId(a.getId());
            item.setBookDeptId(a.getDeptId());
            item.setBookUserId(a.getCustodianId());
            item.setBookLocation(a.getLocation());
            item.setResult(AssetCheckResult.PENDING);
            return item;
        }).toList());
        return check.getId();
    }

    /** 录入盘点结果 */
    @Transactional
    public void record(Long id, Long itemId, AssetCheckRequests.Record req) {
        requireChecking(id);
        FinAssetCheckItem item = item(id, itemId);
        if (item.getAssetId() == null || req.result() == AssetCheckResult.GAIN) {
            throw new BizException("盘盈明细请删除后重新登记");
        }
        boolean moved = req.result() == AssetCheckResult.MOVED;
        if (moved && req.actualDeptId() == null && req.actualUserId() == null && !StringUtils.hasText(req.actualLocation())) {
            throw new BizException("请填写实际的部门、使用人或存放地点");
        }
        // 没有填写的项按账面不变：与账面完全一致时应选择「相符」，否则完成盘点时调拨会因没有变化而失败
        if (moved && (req.actualDeptId() == null || Objects.equals(req.actualDeptId(), item.getBookDeptId()))
                && (req.actualUserId() == null || Objects.equals(req.actualUserId(), item.getBookUserId()))
                && (!StringUtils.hasText(req.actualLocation()) || req.actualLocation().trim().equals(item.getBookLocation()))) {
            throw new BizException("实际的部门、使用人、存放地点与账面相同，请选择「相符」");
        }
        itemMapper.update(Wrappers.<FinAssetCheckItem>lambdaUpdate()
                .set(FinAssetCheckItem::getResult, req.result())
                .set(FinAssetCheckItem::getActualDeptId, moved ? req.actualDeptId() : null)
                .set(FinAssetCheckItem::getActualUserId, moved ? req.actualUserId() : null)
                .set(FinAssetCheckItem::getActualLocation, moved && StringUtils.hasText(req.actualLocation()) ? req.actualLocation().trim() : null)
                .set(FinAssetCheckItem::getRemark, req.remark())
                .set(FinAssetCheckItem::getCheckBy, req.result() == AssetCheckResult.PENDING ? null : SecurityUtils.getUserId())
                .set(FinAssetCheckItem::getCheckTime, req.result() == AssetCheckResult.PENDING ? null : LocalDateTime.now())
                .eq(FinAssetCheckItem::getId, itemId));
    }

    /** 登记盘盈（账外资产） */
    @Transactional
    public void addGain(Long id, AssetCheckRequests.Gain req) {
        requireChecking(id);
        FinAssetCheckItem item = new FinAssetCheckItem();
        item.setCheckId(id);
        item.setResult(AssetCheckResult.GAIN);
        item.setGainName(req.name().trim());
        item.setActualLocation(StringUtils.hasText(req.location()) ? req.location().trim() : null);
        item.setRemark(req.remark());
        item.setCheckBy(SecurityUtils.getUserId());
        item.setCheckTime(LocalDateTime.now());
        itemMapper.insert(item);
    }

    @Transactional
    public void removeGain(Long id, Long itemId) {
        requireChecking(id);
        FinAssetCheckItem item = item(id, itemId);
        if (item.getResult() != AssetCheckResult.GAIN) {
            throw new BizException("只能删除盘盈明细");
        }
        itemMapper.deleteById(itemId);
    }

    /**
     * 完成盘点：全部明细已盘；位置变化的更新卡片，盘亏的处置
     *
     * @return 处置的资产数
     */
    @Transactional
    public int finish(Long id) {
        FinAssetCheck check = requireChecking(id);
        List<FinAssetCheckItem> items = checkMapper.selectItems(id);
        long pending = items.stream().filter(i -> i.getResult() == AssetCheckResult.PENDING).count();
        if (pending > 0) {
            throw new BizException("还有 " + pending + " 项资产没有盘点");
        }
        int disposed = 0;
        for (FinAssetCheckItem i : items) {
            if (i.getResult() == AssetCheckResult.MOVED) {
                FinAsset asset = assetService.get(i.getAssetId());
                assetService.move(asset.getId(), new AssetMoveRequest(AssetMoveType.TRANSFER, check.getCheckDate(),
                        i.getActualDeptId() != null ? i.getActualDeptId() : asset.getDeptId(),
                        i.getActualUserId() != null ? i.getActualUserId() : asset.getCustodianId(),
                        i.getActualLocation(), "盘点 " + check.getCheckNo() + " 位置变化"));
            } else if (i.getResult() == AssetCheckResult.LOSS) {
                assetService.dispose(i.getAssetId(), new DisposeRequest(check.getCheckDate(), null, null, null,
                        "盘点 " + check.getCheckNo() + " 盘亏"));
                disposed++;
            }
        }
        if (checkMapper.update(Wrappers.<FinAssetCheck>lambdaUpdate()
                .set(FinAssetCheck::getStatus, AssetCheckStatus.DONE)
                .set(FinAssetCheck::getFinishTime, LocalDateTime.now())
                .eq(FinAssetCheck::getId, id).eq(FinAssetCheck::getStatus, AssetCheckStatus.CHECKING)) == 0) {
            throw new BizException("盘点单状态已变化，请刷新后重试");
        }
        return disposed;
    }

    @Transactional
    public void delete(Long id) {
        requireChecking(id);
        itemMapper.delete(Wrappers.<FinAssetCheckItem>lambdaQuery().eq(FinAssetCheckItem::getCheckId, id));
        checkMapper.deleteById(id);
    }

    private FinAssetCheckItem item(Long checkId, Long itemId) {
        FinAssetCheckItem item = itemId == null ? null : itemMapper.selectById(itemId);
        if (item == null || !item.getCheckId().equals(checkId)) {
            throw new BizException("盘点明细不存在");
        }
        return item;
    }

    private FinAssetCheck requireChecking(Long id) {
        FinAssetCheck check = id == null ? null : checkMapper.selectById(id);
        if (check == null) {
            throw new BizException("盘点单不存在");
        }
        if (check.getStatus() != AssetCheckStatus.CHECKING) {
            throw new BizException("盘点已完成");
        }
        return check;
    }
}
