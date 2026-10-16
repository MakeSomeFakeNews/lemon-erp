package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.fin.entity.FinAssetMove;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinAssetMoveMapper extends BaseMapper<FinAssetMove> {

    /** 资产的变动履历，新的在前 */
    List<FinAssetMove> selectByAsset(@Param("assetId") Long assetId);
}
