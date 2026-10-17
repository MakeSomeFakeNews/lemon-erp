package com.lemon.erp.modules.bi.service;

import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.bi.dto.BoardRequest;
import com.lemon.erp.modules.bi.dto.ReportRequest;
import com.lemon.erp.modules.bi.entity.BiDashboard;
import com.lemon.erp.modules.bi.entity.BiReport;
import com.lemon.erp.modules.bi.mapper.BiDashboardMapper;
import com.lemon.erp.modules.bi.mapper.BiReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Objects;

/**
 * 自定义报表与仪表盘：报表保存多维分析的配置（数据集、维度、指标、筛选、图表），仪表盘由报表组成。
 * 可以共享给所有人查看，只有创建人可以修改、删除
 */
@Service
@RequiredArgsConstructor
public class BiReportService {

    private static final int CONFIG_MAX = 4000;

    private final BiReportMapper reportMapper;
    private final BiDashboardMapper dashboardMapper;
    private final BiOlapService olapService;
    private final ObjectMapper objectMapper;

    public List<BiReport> reports(String keyword, String cube) {
        return reportMapper.selectVisible(SecurityUtils.getUserId(), keyword, cube);
    }

    /** 本人的或共享的报表 */
    public BiReport report(Long id) {
        BiReport r = id == null ? null : reportMapper.selectById(id);
        if (r == null || (!Objects.equals(r.getCreateBy(), SecurityUtils.getUserId()) && !Integer.valueOf(1).equals(r.getShared()))) {
            throw new BizException("报表不存在或没有共享");
        }
        r.setMine(Objects.equals(r.getCreateBy(), SecurityUtils.getUserId()));
        return r;
    }

    @Transactional
    public Long saveReport(Long id, ReportRequest req) {
        olapService.validate(req.config());
        String config = objectMapper.writeValueAsString(req.config());
        if (config.length() > CONFIG_MAX) {
            throw new BizException("分析配置过长，请减少筛选条件");
        }
        BiReport r = new BiReport();
        r.setName(req.name().trim());
        r.setDataset(req.config().cube());
        r.setConfig(config);
        r.setShared(Integer.valueOf(1).equals(req.shared()) ? 1 : 0);
        r.setRemark(req.remark());
        if (id == null) {
            r.setCreateBy(SecurityUtils.getUserId());
            reportMapper.insert(r);
            return r.getId();
        }
        own(reportMapper.selectById(id));
        r.setId(id);
        reportMapper.updateById(r);
        return id;
    }

    /** 删除报表：仪表盘中引用的不能删除 */
    @Transactional
    public void deleteReport(Long id) {
        own(reportMapper.selectById(id));
        for (BiDashboard d : dashboardMapper.selectList(null)) {
            if (readWidgets(d).stream().anyMatch(w -> id.equals(w.reportId()))) {
                throw new BizException("报表已被仪表盘「{0}」使用，不能删除", d.getName());
            }
        }
        reportMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 仪表盘

    public List<BiDashboard> dashboards() {
        return dashboardMapper.selectVisible(SecurityUtils.getUserId());
    }

    public BiDashboard dashboard(Long id) {
        BiDashboard d = id == null ? null : dashboardMapper.selectById(id);
        if (d == null || (!Objects.equals(d.getCreateBy(), SecurityUtils.getUserId()) && !Integer.valueOf(1).equals(d.getShared()))) {
            throw new BizException("仪表盘不存在或没有共享");
        }
        d.setMine(Objects.equals(d.getCreateBy(), SecurityUtils.getUserId()));
        return d;
    }

    @Transactional
    public Long saveDashboard(Long id, BoardRequest req) {
        req.widgets().forEach(w -> report(w.reportId()));
        BiDashboard d = new BiDashboard();
        d.setName(req.name().trim());
        d.setWidgets(objectMapper.writeValueAsString(req.widgets()));
        d.setShared(Integer.valueOf(1).equals(req.shared()) ? 1 : 0);
        d.setRemark(req.remark());
        if (d.getWidgets().length() > CONFIG_MAX) {
            throw new BizException("组件过多");
        }
        if (id == null) {
            d.setCreateBy(SecurityUtils.getUserId());
            dashboardMapper.insert(d);
            return d.getId();
        }
        own(dashboardMapper.selectById(id));
        d.setId(id);
        dashboardMapper.updateById(d);
        return id;
    }

    @Transactional
    public void deleteDashboard(Long id) {
        own(dashboardMapper.selectById(id));
        dashboardMapper.deleteById(id);
    }

    private List<BoardRequest.Widget> readWidgets(BiDashboard d) {
        return List.of(objectMapper.readValue(d.getWidgets(), BoardRequest.Widget[].class));
    }

    private static void own(Object entity) {
        Long createBy = entity instanceof BiReport r ? r.getCreateBy() : entity instanceof BiDashboard d ? d.getCreateBy() : null;
        if (entity == null) {
            throw new BizException("记录不存在");
        }
        if (!Objects.equals(createBy, SecurityUtils.getUserId())) {
            throw new BizException("只能修改、删除自己创建的");
        }
    }
}
