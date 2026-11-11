package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysPrintTemplate;

import java.util.List;

public interface SysPrintTemplateMapper extends BaseMapper<SysPrintTemplate> {

    /** 上传了 JRXML 的格式 ID */
    List<Long> selectJrxmlIds();

    /** 锁定某个模板标识的全部格式（新增、修改、删除格式时串行执行，保证只有一份默认格式），返回格式 ID */
    List<Long> lockByKey(String templateKey);
}
