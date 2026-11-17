package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsTransferDiff;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WmsTransferDiffMapper extends BaseMapper<WmsTransferDiff> {

    /** 调拨单的差异，带物料、批次、验收单号、处理生成的单据号，按登记先后 */
    List<WmsTransferDiff> selectByDocId(@Param("docId") Long docId);
}
