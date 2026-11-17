package com.lemon.erp.modules.wms.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.wms.entity.WmsTransferReceipt;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface WmsTransferReceiptMapper extends BaseMapper<WmsTransferReceipt> {

    /** 调拨单的验收单，带验收人名称，按验收先后 */
    List<WmsTransferReceipt> selectByDocId(@Param("docId") Long docId);
}
