package com.lemon.erp.modules.workflow.engine;

import org.flowable.common.engine.impl.identity.Authentication;

import java.util.function.Supplier;

/**
 * 以指定用户的身份调用流程引擎：发起人（startUserId）、审批意见的作者等都取自这里。
 * Flowable 使用线程变量保存当前用户，调用结束后必须恢复
 */
public final class FlowableUserContext {

    private FlowableUserContext() {
    }

    public static <T> T callAs(Long userId, Supplier<T> action) {
        String previous = Authentication.getAuthenticatedUserId();
        Authentication.setAuthenticatedUserId(String.valueOf(userId));
        try {
            return action.get();
        } finally {
            Authentication.setAuthenticatedUserId(previous);
        }
    }

    public static void runAs(Long userId, Runnable action) {
        callAs(userId, () -> {
            action.run();
            return null;
        });
    }
}
