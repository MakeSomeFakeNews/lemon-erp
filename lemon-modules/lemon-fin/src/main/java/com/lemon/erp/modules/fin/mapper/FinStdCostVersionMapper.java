package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinStdCostVersion;
import org.apache.ibatis.annotations.Param;

public interface FinStdCostVersionMapper extends BaseMapper<FinStdCostVersion> {

    IPage<FinStdCostVersion> selectPageList(IPage<FinStdCostVersion> page);

    FinStdCostVersion selectDetail(@Param("id") Long id);

    /** 期间使用的版本：生效期间不晚于该期间的最近一个已发布版本 */
    FinStdCostVersion selectEffective(@Param("period") int period);
}
