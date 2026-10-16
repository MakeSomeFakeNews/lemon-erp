package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.scm.entity.ScmBid;
import com.lemon.erp.modules.scm.entity.ScmBidItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmBidMapper extends BaseMapper<ScmBid> {

    List<ScmBid> selectByTender(@Param("tenderId") Long tenderId);

    List<ScmBidItem> selectItems(@Param("bidIds") List<Long> bidIds);

    int insertItems(@Param("items") List<ScmBidItem> items);

    int deleteItems(@Param("bidId") Long bidId);
}
