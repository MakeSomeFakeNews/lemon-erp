package com.lemon.erp.modules.fin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.lemon.erp.modules.fin.entity.FinNote;
import com.lemon.erp.modules.fin.model.NoteKind;
import com.lemon.erp.modules.fin.model.NoteStatus;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface FinNoteMapper extends BaseMapper<FinNote> {

    IPage<FinNote> selectPageList(IPage<FinNote> page, @Param("kind") NoteKind kind, @Param("status") NoteStatus status,
                                  @Param("keyword") String keyword, @Param("dueBefore") LocalDate dueBefore);

    FinNote selectDetail(@Param("id") Long id);

    /** 到期日在 dueBefore 之前（含）仍未处理的票据：应收票据持有中、应付票据未兑付 */
    List<FinNote> selectDue(@Param("dueBefore") LocalDate dueBefore);

    /** 状态变更（乐观校验原状态），返回更新行数 */
    int transit(@Param("id") Long id, @Param("from") NoteStatus from, @Param("to") NoteStatus to);
}
