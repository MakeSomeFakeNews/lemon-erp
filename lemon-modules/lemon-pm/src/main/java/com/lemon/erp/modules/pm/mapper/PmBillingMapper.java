package com.lemon.erp.modules.pm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.pm.entity.PmBilling;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PmBillingMapper extends BaseMapper<PmBilling> {

    List<PmBilling> selectByProject(@Param("projectId") Long projectId);
}
