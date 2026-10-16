package com.lemon.erp.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lemon.erp.modules.system.entity.SysPrintTemplate;

import java.util.List;

public interface SysPrintTemplateMapper extends BaseMapper<SysPrintTemplate> {

    /** 上传了 JRXML 的模板标识 */
    List<String> selectJrxmlKeys();
}
