package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.QmsNcr;
import com.lemon.erp.modules.mes.model.NcrStatus;
import org.apache.ibatis.annotations.Param;

public interface QmsNcrMapper extends BaseMapper<QmsNcr> {

    IPage<QmsNcr> selectPageList(IPage<QmsNcr> page, @Param("status") NcrStatus status, @Param("keyword") String keyword);

    QmsNcr selectDetail(@Param("id") Long id);

    /** 状态变更（校验原状态），返回更新行数 */
    int transit(@Param("id") Long id, @Param("from") NcrStatus from, @Param("to") NcrStatus to);
}
