package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.MesWoSupplementItem;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

public interface MesWoSupplementItemMapper extends BaseMapper<MesWoSupplementItem> {

    List<MesWoSupplementItem> selectBySupp(@Param("suppId") Long suppId);

    void insertBatch(@Param("list") List<MesWoSupplementItem> list);

    /** 发料：累计不超过申请数量 */
    int addIssued(@Param("id") Long id, @Param("qty") BigDecimal qty);
}
