package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmShipmentTrack;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmShipmentTrackMapper extends BaseMapper<ScmShipmentTrack> {

    /** 物流动态，按时间倒序 */
    List<ScmShipmentTrack> selectByShipment(@Param("shipmentId") Long shipmentId);
}
