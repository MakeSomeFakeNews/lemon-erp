package com.lemon.erp.modules.mes.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lemon.erp.common.exception.BizException;
import com.lemon.erp.common.i18n.Messages;
import com.lemon.erp.common.result.PageResult;
import com.lemon.erp.framework.data.StatusTransitions;
import com.lemon.erp.framework.dict.DictLookup;
import com.lemon.erp.framework.security.SecurityUtils;
import com.lemon.erp.modules.mes.dto.PlmRequests;
import com.lemon.erp.modules.mes.entity.MesTechDoc;
import com.lemon.erp.modules.mes.mapper.MesTechDocMapper;
import com.lemon.erp.modules.mes.model.DocStatus;
import com.lemon.erp.modules.wms.service.DocNoService;
import com.lemon.erp.modules.wms.service.WmsMaterialService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 技术文档：草稿 → 发布（须上传文件），发布新版本时上一版本作废；已发布的最新版本可以升版（复制为新的草稿版本）
 */
@Service
@RequiredArgsConstructor
public class MesTechDocService {

    /** 文档编号的编码规则标识 */
    public static final String DOC_NO = "TD";

    private final MesTechDocMapper docMapper;
    private final WmsMaterialService materialService;
    private final DocNoService docNoService;
    private final DictLookup dictLookup;

    /** @param latest 只列每个文档的最新版本 */
    public PageResult<MesTechDoc> page(long current, long size, String docType, DocStatus status, Long materialId, String keyword,
                                       boolean latest) {
        Page<MesTechDoc> page = Page.of(current, size);
        docMapper.selectPageList(page, docType, status, materialId, keyword, latest);
        return PageResult.of(page);
    }

    public MesTechDoc detail(Long id) {
        MesTechDoc doc = id == null ? null : docMapper.selectDetail(id);
        if (doc == null) {
            throw new BizException("mes.error.technicalDocumentDoesNotExist");
        }
        return doc;
    }

    public List<MesTechDoc> versions(Long id) {
        return docMapper.selectVersions(detail(id).getDocNo());
    }

    /** 物料的已发布文档（BOM 详情显示产品和子件的文档） */
    public List<MesTechDoc> released(Collection<Long> materialIds) {
        return materialIds.isEmpty() ? List.of() : docMapper.selectReleased(materialIds);
    }

    @Transactional
    public Long create(PlmRequests.Doc req) {
        MesTechDoc doc = new MesTechDoc();
        doc.setDocNo(docNoService.next(DOC_NO));
        apply(doc, req);
        doc.setStatus(DocStatus.DRAFT);
        doc.setCreateBy(SecurityUtils.getUserId());
        save(doc);
        return doc.getId();
    }

    @Transactional
    public void update(Long id, PlmRequests.Doc req) {
        MesTechDoc doc = requireStatus(id, DocStatus.DRAFT, "mes.label.edit");
        apply(doc, req);
        try {
            docMapper.updateById(doc);
        } catch (DuplicateKeyException e) {
            throw new BizException("mes.error.documentAlreadyHasVersion", req.version().trim());
        }
    }

    @Transactional
    public void delete(Long id) {
        requireStatus(id, DocStatus.DRAFT, "mes.label.delete");
        docMapper.deleteById(id);
    }

    /** 发布：须上传文件；上一版本作废 */
    @Transactional
    public void release(Long id) {
        MesTechDoc doc = requireStatus(id, DocStatus.DRAFT, "mes.label.release");
        if (doc.getFileCount() == null || doc.getFileCount() == 0) {
            throw new BizException("mes.error.uploadDocumentFilesFirst");
        }
        if (!StatusTransitions.tryChange(docMapper, MesTechDoc::getId, id, MesTechDoc::getStatus, DocStatus.DRAFT, DocStatus.RELEASED,
                w -> w.set(MesTechDoc::getReleaseBy, SecurityUtils.getUserId()).set(MesTechDoc::getReleaseTime, LocalDateTime.now()))) {
            throw new BizException("mes.error.documentStatusHasChangedRefreshTry");
        }
        if (doc.getPrevId() != null) {
            docMapper.update(Wrappers.<MesTechDoc>lambdaUpdate().set(MesTechDoc::getStatus, DocStatus.OBSOLETE)
                    .eq(MesTechDoc::getId, doc.getPrevId()).eq(MesTechDoc::getStatus, DocStatus.RELEASED));
        }
    }

    /** 升版：已发布的最新版本复制为新的草稿版本（文件在新版本中重新上传） */
    @Transactional
    public Long revise(Long id, PlmRequests.Revise req) {
        MesTechDoc doc = requireStatus(id, DocStatus.RELEASED, "mes.label.revise");
        if (docMapper.exists(Wrappers.<MesTechDoc>lambdaQuery().eq(MesTechDoc::getPrevId, id))) {
            throw new BizException("mes.error.newerVersionAlreadyExists");
        }
        MesTechDoc next = new MesTechDoc();
        next.setDocNo(doc.getDocNo());
        next.setName(doc.getName());
        next.setDocType(doc.getDocType());
        next.setMaterialId(doc.getMaterialId());
        next.setVersion(req.version().trim());
        next.setEcnId(req.ecnId());
        next.setDescription(req.description());
        next.setPrevId(id);
        next.setStatus(DocStatus.DRAFT);
        next.setCreateBy(SecurityUtils.getUserId());
        save(next);
        return next.getId();
    }

    private void apply(MesTechDoc doc, PlmRequests.Doc req) {
        dictLookup.check("mes_doc_type", req.docType(), "mes.label.docType");
        if (req.materialId() != null) {
            materialService.get(req.materialId());
        }
        doc.setName(req.name().trim());
        doc.setDocType(req.docType());
        doc.setMaterialId(req.materialId());
        doc.setVersion(req.version().trim());
        doc.setEcnId(req.ecnId());
        doc.setDescription(req.description());
    }

    private void save(MesTechDoc doc) {
        try {
            docMapper.insert(doc);
        } catch (DuplicateKeyException e) {
            throw new BizException("mes.error.documentAlreadyHasVersion", doc.getVersion());
        }
    }

    private MesTechDoc requireStatus(Long id, DocStatus status, String action) {
        MesTechDoc doc = detail(id);
        if (doc.getStatus() != status) {
            throw new BizException("mes.error.currentDocumentStatusDoesNotAllow", Messages.get(action));
        }
        return doc;
    }
}
