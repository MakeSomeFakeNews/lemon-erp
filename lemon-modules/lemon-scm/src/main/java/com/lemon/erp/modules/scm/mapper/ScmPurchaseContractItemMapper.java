package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmPurchaseContractItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmPurchaseContractItemMapper extends BaseMapper<ScmPurchaseContractItem> {

    List<ScmPurchaseContractItem> selectByContract(@Param("contractId") Long contractId);

    /** 批量插入，单条 INSERT ... VALUES (...), (...) */
    int insertBatch(@Param("items") List<ScmPurchaseContractItem> items);
}
