package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.entity.ScmRequisition;
import com.lemon.erp.modules.scm.entity.ScmRequisitionItem;
import com.lemon.erp.modules.scm.model.RequisitionStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmRequisitionMapper extends BaseMapper<ScmRequisition> {

    IPage<ScmRequisition> selectPageList(IPage<ScmRequisition> page, @Param("keyword") String keyword, @Param("status") RequisitionStatus status);

    ScmRequisition selectDetail(@Param("id") Long id);

    List<ScmRequisitionItem> selectItems(@Param("requisitionId") Long requisitionId);
}
