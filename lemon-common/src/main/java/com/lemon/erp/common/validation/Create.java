package com.lemon.erp.common.validation;

import jakarta.validation.groups.Default;

/**
 * 校验分组：新增。继承 Default，未指定分组的约束同样生效
 */
public interface Create extends Default {
}
