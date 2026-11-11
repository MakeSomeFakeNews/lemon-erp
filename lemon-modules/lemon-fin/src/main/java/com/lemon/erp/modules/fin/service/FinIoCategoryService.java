package com.lemon.erp.modules.fin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lemon.erp.common.enums.EnableStatus;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.modules.fin.dto.IoRequests;
import com.lemon.erp.modules.fin.entity.FinIoCategory;
import com.lemon.erp.modules.fin.entity.FinIoDocItem;
import com.lemon.erp.modules.fin.mapper.FinIoCategoryMapper;
import com.lemon.erp.modules.fin.mapper.FinIoDocItemMapper;
import com.lemon.erp.modules.fin.model.FundDirection;
import com.lemon.erp.modules.fin.model.IoCategoryCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 收支类别：每个租户一份（新租户从平台复制内置类别），一级，标明收 / 支并关联凭证科目。
 * 同一方向下名称不能重复；已被收支单使用的不能删除、不能改方向，可以停用（停用后新单据不能再选）。
 * 内置类别（有标识 code，如销售运费、代垫运费，业务单据自动生成费用支出单时使用）不能删除、不能改方向，名称和科目可以修改
 */
@Service
@RequiredArgsConstructor
public class FinIoCategoryService {

    private final FinIoCategoryMapper categoryMapper;
    private final FinIoDocItemMapper itemMapper;
    private final FinAccountService accountService;

    public List<FinIoCategory> list(FundDirection direction, EnableStatus status) {
        return categoryMapper.selectViewList(direction, status);
    }

    /** 按内置标识取类别（业务单据自动生成费用支出单时使用，停用的也可以用） */
    public FinIoCategory getBuiltin(IoCategoryCode code) {
        FinIoCategory c = categoryMapper.selectOne(Wrappers.<FinIoCategory>lambdaQuery().eq(FinIoCategory::getCode, code));
        if (c == null) {
            throw new BizException("fin.error.ioCategoryBuiltinMissing", code.name());
        }
        return c;
    }

    public FinIoCategory get(Long id) {
        FinIoCategory c = id == null ? null : categoryMapper.selectById(id);
        if (c == null) {
            throw new BizException("fin.error.ioCategoryNotFound");
        }
        return c;
    }

    /** @return 新类别 ID */
    public Long create(IoRequests.CategorySave req) {
        validate(null, req);
        FinIoCategory c = new FinIoCategory();
        fill(c, req);
        categoryMapper.insert(c);
        return c.getId();
    }

    public void update(Long id, IoRequests.CategorySave req) {
        FinIoCategory c = get(id);
        if (c.getDirection() != req.direction() && c.getCode() != null) {
            throw new BizException("fin.error.ioCategoryBuiltinDirection", c.getName());
        }
        if (c.getDirection() != req.direction() && used(id)) {
            throw new BizException("fin.error.ioCategoryUsedDirection", c.getName());
        }
        validate(id, req);
        fill(c, req);
        categoryMapper.updateById(c);
    }

    public void delete(List<Long> ids) {
        for (Long id : ids) {
            FinIoCategory c = get(id);
            if (c.getCode() != null) {
                throw new BizException("fin.error.ioCategoryBuiltinDelete", c.getName());
            }
            if (used(id)) {
                throw new BizException("fin.error.ioCategoryUsed", c.getName());
            }
        }
        categoryMapper.deleteByIds(ids);
    }

    private boolean used(Long id) {
        return itemMapper.exists(Wrappers.<FinIoDocItem>lambdaQuery().eq(FinIoDocItem::getCategoryId, id));
    }

    private void validate(Long selfId, IoRequests.CategorySave req) {
        String name = req.name().trim();
        if (categoryMapper.exists(Wrappers.<FinIoCategory>lambdaQuery().eq(FinIoCategory::getDirection, req.direction())
                .eq(FinIoCategory::getName, name).ne(selfId != null, FinIoCategory::getId, selfId))) {
            throw new BizException("fin.error.ioCategoryNameExists", name);
        }
        accountService.requireLeaves(List.of(req.accountId()));
    }

    private static void fill(FinIoCategory c, IoRequests.CategorySave req) {
        c.setName(req.name().trim());
        c.setDirection(req.direction());
        c.setAccountId(req.accountId());
        c.setSort(req.sort() == null ? 0 : req.sort());
        c.setStatus(req.status() == null ? EnableStatus.ENABLED : req.status());
        c.setRemark(req.remark());
    }
}
