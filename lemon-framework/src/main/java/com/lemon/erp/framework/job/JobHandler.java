package com.lemon.erp.framework.job;

/**
 * 定时任务：业务模块实现并注册为 Bean，系统模块（{@code SysJobService}）按「定时任务」页面中的 Cron 调度、记录执行日志，
 * 也可以手动执行。任务在没有登录用户的线程中运行，不要依赖当前用户（数据权限、制单人等）
 */
public interface JobHandler {

    /** 唯一标识，如 crm.followRemind */
    String key();

    String name();

    /** 默认 Cron（Spring 6 段格式），首次启动时写入任务表，之后以页面设置为准 */
    String defaultCron();

    /** 首次建立任务时是否启用 */
    default boolean defaultEnabled() {
        return true;
    }

    /** 默认参数，如 days=30 */
    default String defaultParams() {
        return null;
    }

    /**
     * 执行
     *
     * @param params 页面设置的参数（key=value，多个用逗号分隔）
     * @return 执行结果说明，写入日志
     */
    String run(JobParams params);
}
