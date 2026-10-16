package com.lemon.erp.modules.scm.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.scm.dto.SrmViews;
import com.lemon.erp.modules.scm.entity.ScmAsn;
import com.lemon.erp.modules.scm.entity.ScmAsnItem;
import com.lemon.erp.modules.scm.model.AsnStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ScmAsnMapper extends BaseMapper<ScmAsn> {

    /** @param supplierId 供应商门户只看本供应商，内部为空 */
    IPage<ScmAsn> selectPageList(IPage<ScmAsn> page, @Param("supplierId") Long supplierId, @Param("status") AsnStatus status,
                                 @Param("keyword") String keyword);

    ScmAsn selectDetail(@Param("id") Long id);

    List<ScmAsnItem> selectItems(@Param("asnId") Long asnId);

    /** 订单各明细的未送货数量（排除指定送货通知） */
    List<SrmViews.AsnOpenLine> selectOpenLines(@Param("orderId") Long orderId, @Param("excludeAsnId") Long excludeAsnId);

    int insertItems(@Param("items") List<ScmAsnItem> items);

    int transit(@Param("id") Long id, @Param("from") AsnStatus from, @Param("to") AsnStatus to);
}
