package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinAsset;
import com.lemon.erp.modules.fin.entity.FinAssetCheck;
import com.lemon.erp.modules.fin.entity.FinAssetCheckItem;
import com.lemon.erp.modules.fin.model.AssetCheckStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinAssetCheckMapper extends BaseMapper<FinAssetCheck> {

    IPage<FinAssetCheck> selectPageList(IPage<FinAssetCheck> page, @Param("status") AssetCheckStatus status);

    FinAssetCheck selectDetail(@Param("id") Long id);

    List<FinAssetCheckItem> selectItems(@Param("checkId") Long checkId);

    /** 盘点范围内未处置的资产 */
    List<FinAsset> selectScope(@Param("deptId") Long deptId, @Param("location") String location);

    int insertItems(@Param("items") List<FinAssetCheckItem> items);
}
