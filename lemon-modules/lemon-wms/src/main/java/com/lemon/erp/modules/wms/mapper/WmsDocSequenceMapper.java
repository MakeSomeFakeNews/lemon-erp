package com.lemon.erp.modules.wms.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * 单据号流水
 */
public interface WmsDocSequenceMapper {

    /** 流水号加 1，返回影响行数（当天第一张单据时为 0） */
    int increment(@Param("key") String key);

    /** 当天第一张单据，流水号为 1；并发时可能主键冲突，由调用方重试 increment */
    void insertFirst(@Param("key") String key);

    long selectValue(@Param("key") String key);
}
