package com.lemon.erp.modules.mes.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.mes.entity.MesBomOp;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MesBomOpMapper extends BaseMapper<MesBomOp> {

    List<MesBomOp> selectByBomId(@Param("bomId") Long bomId);

    void insertBatch(@Param("list") List<MesBomOp> list);
}
