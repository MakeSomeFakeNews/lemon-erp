package com.lemon.erp.modules.hr.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.modules.hr.entity.HrEmployee;
import com.lemon.erp.modules.hr.entity.HrPosition;
import com.lemon.erp.modules.hr.mapper.HrEmployeeMapper;
import com.lemon.erp.modules.hr.mapper.HrPositionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class HrPositionService {

    private final HrPositionMapper positionMapper;
    private final HrEmployeeMapper employeeMapper;

    public PageResult<HrPosition> page(long current, long size, String keyword, String category, Integer status) {
        Page<HrPosition> page = Page.of(current, size);
        positionMapper.selectPageList(page, keyword, category, status);
        return PageResult.of(page);
    }

    /** 启用的职位，下拉选择 */
    public List<HrPosition> options() {
        return positionMapper.selectList(Wrappers.<HrPosition>lambdaQuery()
                .select(HrPosition::getId, HrPosition::getCode, HrPosition::getName, HrPosition::getCategory, HrPosition::getGrade)
                .eq(HrPosition::getStatus, 1)
                .orderByAsc(HrPosition::getSort, HrPosition::getId));
    }

    public HrPosition get(Long id) {
        HrPosition position = id == null ? null : positionMapper.selectById(id);
        if (position == null) {
            throw new BizException("职位不存在");
        }
        return position;
    }

    public void create(HrPosition position) {
        checkCodeUnique(position.getCode(), null);
        position.setId(null);
        if (position.getStatus() == null) position.setStatus(1);
        if (position.getSort() == null) position.setSort(0);
        positionMapper.insert(position);
    }

    public void update(Long id, HrPosition position) {
        get(id);
        checkCodeUnique(position.getCode(), id);
        position.setId(id);
        if (position.getSort() == null) position.setSort(0);
        positionMapper.updateById(position);
    }

    public void changeStatus(Long id, Integer status) {
        get(id);
        HrPosition entity = new HrPosition();
        entity.setId(id);
        entity.setStatus(status);
        positionMapper.updateById(entity);
    }

    /** 有员工（含离职员工）的职位不能删除，可以停用 */
    public void delete(List<Long> ids) {
        for (HrPosition p : positionMapper.selectByIds(ids)) {
            if (employeeMapper.exists(Wrappers.<HrEmployee>lambdaQuery().eq(HrEmployee::getPositionId, p.getId()))) {
                throw new BizException("职位「{0}」已有员工，不能删除，可以停用", p.getName());
            }
        }
        positionMapper.deleteByIds(ids);
    }

    private void checkCodeUnique(String code, Long id) {
        HrPosition exist = positionMapper.selectOne(Wrappers.<HrPosition>lambdaQuery().eq(HrPosition::getCode, code));
        if (exist != null && !Objects.equals(exist.getId(), id)) {
            throw new BizException("职位编码已存在");
        }
    }
}
