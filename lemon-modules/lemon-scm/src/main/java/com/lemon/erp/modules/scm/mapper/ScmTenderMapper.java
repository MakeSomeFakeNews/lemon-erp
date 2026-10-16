package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmTender;
import com.lemon.erp.modules.scm.entity.ScmTenderItem;
import com.lemon.erp.modules.scm.entity.ScmTenderSupplier;
import com.lemon.erp.modules.scm.model.TenderStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmTenderMapper extends BaseMapper<ScmTender> {

    IPage<ScmTender> selectPageList(IPage<ScmTender> page, @Param("status") TenderStatus status, @Param("keyword") String keyword);

    ScmTender selectDetail(@Param("id") Long id);

    List<ScmTenderItem> selectItems(@Param("tenderId") Long tenderId);

    /** 邀请的供应商，含是否已投标 */
    List<ScmTenderSupplier> selectSuppliers(@Param("tenderId") Long tenderId);

    /** 供应商门户：邀请了该供应商、已发布的招标 */
    IPage<ScmTender> selectForSupplier(IPage<ScmTender> page, @Param("supplierId") Long supplierId);

    int transit(@Param("id") Long id, @Param("from") TenderStatus from, @Param("to") TenderStatus to);

    int insertItems(@Param("items") List<ScmTenderItem> items);

    /** 删除招标的物料、邀请的供应商（草稿修改、删除） */
    int deleteItems(@Param("tenderId") Long tenderId);

    int deleteSuppliers(@Param("tenderId") Long tenderId);

    int insertSuppliers(@Param("tenderId") Long tenderId, @Param("supplierIds") List<Long> supplierIds);
}
