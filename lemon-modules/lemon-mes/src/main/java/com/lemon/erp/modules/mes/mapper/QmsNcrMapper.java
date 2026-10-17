package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.mes.entity.QmsNcr;
import com.lemon.erp.modules.mes.model.NcrStatus;
import org.apache.ibatis.annotations.Param;

public interface QmsNcrMapper extends BaseMapper<QmsNcr> {

    IPage<QmsNcr> selectPageList(IPage<QmsNcr> page, @Param("status") NcrStatus status, @Param("keyword") String keyword);

    QmsNcr selectDetail(@Param("id") Long id);
}
