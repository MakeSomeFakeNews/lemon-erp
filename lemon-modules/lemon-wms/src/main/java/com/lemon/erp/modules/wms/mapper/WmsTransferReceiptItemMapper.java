package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsTransferReceiptItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WmsTransferReceiptItemMapper extends BaseMapper<WmsTransferReceiptItem> {

    /** 调拨单全部验收单的明细，带物料、批次、调入库位 */
    List<WmsTransferReceiptItem> selectByDocId(@Param("docId") Long docId);

    void insertBatch(@Param("list") List<WmsTransferReceiptItem> list);
}
