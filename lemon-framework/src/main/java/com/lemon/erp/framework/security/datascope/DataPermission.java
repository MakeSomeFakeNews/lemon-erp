package com.lemon.erp.framework.security.datascope;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明式数据权限：标注的方法执行期间，对 {@link #table()} 的查询会自动追加当前用户的数据权限条件
 * <pre>{@code
 * @DataPermission(table = "biz_order", deptColumn = "dept_id", userColumn = "create_by")
 * public PageResult<BizOrder> page(...) { ... }
 * }</pre>
 * 生成的条件形如 {@code AND (biz_order.dept_id IN (...) OR biz_order.create_by = 当前用户)}，
 * 列表与分页 count 都会生效。
 * <p>注意：基于 Spring AOP，同一个类内部的方法互相调用不会生效；需要在代码中局部启用时使用
 * {@link DataPermissionContext#execute(DataPermissionRule, java.util.function.Supplier)}。
 * 只建议标注在查询方法上，写操作中的唯一性校验等查询不应受数据权限影响。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {

    /** 需要过滤的表名 */
    String table();

    /** 数据所属部门列，为空表示不按部门过滤 */
    String deptColumn() default "dept_id";

    /** 数据所属用户列（创建人 / 负责人等），用于「仅本人数据」，为空表示不按用户过滤 */
    String userColumn() default "create_by";
}
