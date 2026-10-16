package com.lemon.erp.framework.ref;

/**
 * 主数据被引用检查：删除物料、仓库、客户、供应商、用户、部门前，各业务模块检查自己的单据是否引用了它。
 * 上游模块（如仓库）看不到下游模块（如生产、客户关系）的表，由下游模块实现本接口注册为 Bean
 */
public interface ReferenceChecker {

    /**
     * @return 引用它的单据名称（如「BOM」「采购订单」），没有引用返回 null
     */
    String findReference(RefType type, Long id);
}
