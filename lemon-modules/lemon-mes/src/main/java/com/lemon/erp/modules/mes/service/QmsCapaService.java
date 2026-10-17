package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.message.MessageSender;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.dto.QmsRequests;
import com.lemon.erp.modules.mes.entity.QmsCapa;
import com.lemon.erp.modules.mes.entity.QmsCapaStep;
import com.lemon.erp.modules.mes.entity.QmsNcr;
import com.lemon.erp.modules.mes.mapper.QmsCapaMapper;
import com.lemon.erp.modules.mes.mapper.QmsCapaStepMapper;
import com.lemon.erp.modules.mes.mapper.QmsNcrMapper;
import com.lemon.erp.modules.mes.model.CapaSource;
import com.lemon.erp.modules.mes.model.CapaStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

/**
 * 纠正预防（8D）：创建时生成 D1 ~ D8 步骤，各步骤填写内容并标记完成；全部完成后提交效果验证，
 * 验证有效关闭，无效退回处理中
 */
@Service
@RequiredArgsConstructor
public class QmsCapaService {

    /** 纠正预防单号的编码规则标识 */
    public static final String DOC_NO = "CP";
    /** 8D 步骤数 */
    private static final int STEPS = 8;

    private final QmsCapaMapper capaMapper;
    private final QmsCapaStepMapper stepMapper;
    private final QmsNcrMapper ncrMapper;
    private final DocNoService docNoService;
    private final MessageSender messageSender;

    public PageResult<QmsCapa> page(long current, long size, CapaStatus status, String keyword) {
        Page<QmsCapa> page = Page.of(current, size);
        capaMapper.selectPageList(page, status, keyword);
        return PageResult.of(page);
    }

    public QmsCapa detail(Long id) {
        QmsCapa capa = id == null ? null : capaMapper.selectDetail(id);
        if (capa == null) {
            throw new BizException("纠正预防单不存在");
        }
        capa.setSteps(stepMapper.selectByCapa(id));
        return capa;
    }

    @Transactional
    public Long create(QmsRequests.Capa req) {
        QmsCapa capa = new QmsCapa();
        capa.setCapaNo(docNoService.next(DOC_NO));
        apply(capa, req);
        capa.setStatus(CapaStatus.OPEN);
        capa.setCreateBy(SecurityUtils.getUserId());
        capaMapper.insert(capa);
        stepMapper.insertBatch(IntStream.rangeClosed(1, STEPS).mapToObj(i -> {
            QmsCapaStep s = new QmsCapaStep();
            s.setCapaId(capa.getId());
            s.setStep("D" + i);
            s.setOwnerId(req.ownerId());
            return s;
        }).toList());
        messageSender.send(List.of(req.ownerId()), MessageSender.Message.of("REMIND", "纠正预防待处理：" + capa.getCapaNo(), // i18n-ignore
                capa.getTitle() + "，要求 " + capa.getDueDate() + " 前完成", "/qms/capa")); // i18n-ignore
        return capa.getId();
    }

    @Transactional
    public void update(Long id, QmsRequests.Capa req) {
        QmsCapa capa = requireStatus(id, CapaStatus.OPEN, "修改");
        apply(capa, req);
        capaMapper.updateById(capa);
    }

    /** 填写步骤：处理中才能修改 */
    @Transactional
    public void saveStep(Long id, Long stepId, QmsRequests.CapaStep req) {
        requireStatus(id, CapaStatus.OPEN, "填写步骤");
        QmsCapaStep step = stepMapper.selectById(stepId);
        if (step == null || !step.getCapaId().equals(id)) {
            throw new BizException("步骤不属于该纠正预防单");
        }
        boolean done = Boolean.TRUE.equals(req.done());
        if (done && (req.content() == null || req.content().isBlank())) {
            throw new BizException("{0} 请先填写内容再标记完成", step.getStep());
        }
        stepMapper.update(Wrappers.<QmsCapaStep>lambdaUpdate()
                .set(QmsCapaStep::getContent, req.content())
                .set(QmsCapaStep::getOwnerId, req.ownerId())
                .set(QmsCapaStep::getDone, done ? 1 : 0)
                .set(QmsCapaStep::getDoneDate, done ? (step.getDoneDate() != null ? step.getDoneDate() : LocalDate.now()) : null)
                .eq(QmsCapaStep::getId, stepId));
    }

    /** 提交效果验证：全部步骤完成后 */
    @Transactional
    public void submitVerify(Long id) {
        requireStatus(id, CapaStatus.OPEN, "提交验证");
        List<QmsCapaStep> steps = stepMapper.selectByCapa(id);
        List<String> undone = steps.stream().filter(s -> s.getDone() == null || s.getDone() != 1).map(QmsCapaStep::getStep).toList();
        if (!undone.isEmpty()) {
            throw new BizException("步骤 {0} 尚未完成", String.join(I18n.t("、"), undone));
        }
        transit(id, CapaStatus.OPEN, CapaStatus.VERIFYING, null);
    }

    /** 效果验证：有效关闭，无效退回处理中（保留验证说明） */
    @Transactional
    public void verify(Long id, QmsRequests.CapaVerify req) {
        requireStatus(id, CapaStatus.VERIFYING, "验证");
        transit(id, CapaStatus.VERIFYING, req.effective() ? CapaStatus.CLOSED : CapaStatus.OPEN, req.verifyResult());
    }

    /** 删除：处理中的 */
    @Transactional
    public void delete(Long id) {
        requireStatus(id, CapaStatus.OPEN, "删除");
        stepMapper.delete(Wrappers.<QmsCapaStep>lambdaQuery().eq(QmsCapaStep::getCapaId, id));
        capaMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 内部

    private void apply(QmsCapa capa, QmsRequests.Capa req) {
        if (req.source() == CapaSource.NCR) {
            QmsNcr ncr = req.ncrId() == null ? null : ncrMapper.selectById(req.ncrId());
            if (ncr == null) {
                throw new BizException("请选择来源不合格品处理单");
            }
            capa.setNcrId(ncr.getId());
        } else {
            capa.setNcrId(null);
        }
        capa.setSource(req.source());
        capa.setTitle(req.title().trim());
        capa.setDescription(req.description());
        capa.setOwnerId(req.ownerId());
        capa.setDueDate(req.dueDate());
    }

    private void transit(Long id, CapaStatus from, CapaStatus to, String verifyResult) {
        boolean verified = from == CapaStatus.VERIFYING;
        if (capaMapper.update(Wrappers.<QmsCapa>lambdaUpdate()
                .set(QmsCapa::getStatus, to)
                .set(verified, QmsCapa::getVerifyResult, verifyResult)
                .set(verified, QmsCapa::getVerifiedBy, SecurityUtils.getUserId())
                .set(verified, QmsCapa::getVerifyTime, LocalDateTime.now())
                .eq(QmsCapa::getId, id).eq(QmsCapa::getStatus, from)) == 0) {
            throw new BizException("纠正预防单状态已变化，请刷新后重试");
        }
    }

    private QmsCapa requireStatus(Long id, CapaStatus status, String action) {
        QmsCapa capa = capaMapper.selectById(id);
        if (capa == null) {
            throw new BizException("纠正预防单不存在");
        }
        if (capa.getStatus() != status) {
            throw new BizException("纠正预防单当前状态不能{0}", I18n.t(action));
        }
        return capa;
    }
}
