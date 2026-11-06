package com.lemon.erp.modules.ai.client;

/** 流式对话的回调，在调用线程中依次调用 */
@FunctionalInterface
public interface LlmStreamListener {

    /** 回答文字的增量（不含工具调用） */
    void onDelta(String text);

    /** 返回 true 时停止读取（用户点了「停止生成」或连接已断开），已收到的内容照常返回 */
    default boolean isCancelled() {
        return false;
    }
}
