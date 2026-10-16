package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.MesTechDoc;
import com.lemon.erp.modules.mes.model.DocStatus;
import com.lemon.erp.modules.mes.model.DocType;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

public interface MesTechDocMapper extends BaseMapper<MesTechDoc> {

    /** @param latest 只列每个文档的最新版本 */
    IPage<MesTechDoc> selectPageList(IPage<MesTechDoc> page, @Param("docType") DocType docType, @Param("status") DocStatus status,
                                     @Param("materialId") Long materialId, @Param("keyword") String keyword, @Param("latest") boolean latest);

    MesTechDoc selectDetail(@Param("id") Long id);

    /** 同一文档的全部版本，新的在前 */
    List<MesTechDoc> selectVersions(@Param("docNo") String docNo);

    /** 物料的已发布文档 */
    List<MesTechDoc> selectReleased(@Param("materialIds") Collection<Long> materialIds);
}
