package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.ShipmentQuery;
import com.lemon.erp.modules.scm.entity.ScmShipment;
import org.apache.ibatis.annotations.Param;

public interface ScmShipmentMapper extends BaseMapper<ScmShipment> {

    IPage<ScmShipment> selectPageList(IPage<ScmShipment> page, @Param("q") ShipmentQuery query);

    ScmShipment selectDetail(@Param("id") Long id);
}
