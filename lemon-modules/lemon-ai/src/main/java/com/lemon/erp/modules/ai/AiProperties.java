package com.lemon.erp.modules.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * AI 模块的平台级参数（lemon.ai.*）。服务商、密钥、模型等按租户在「AI 设置」中配置，不在这里
 */
@Data
@ConfigurationProperties(prefix = "lemon.ai")
public class AiProperties {

    /** 连接超时 */
    private Duration connectTimeout = Duration.ofSeconds(10);

    /** 等待模型开始响应的超时（非流式为整个回答） */
    private Duration readTimeout = Duration.ofSeconds(120);

    /** 429 / 5xx / 连接失败时的最多重试次数 */
    private int maxRetries = 2;

    /** 第一次重试前的等待时间，之后每次翻倍 */
    private Duration retryBackoff = Duration.ofSeconds(1);

    /** 一次提问中工具调用的最多轮数，防止死循环 */
    private int maxToolRounds = 6;

    /** 发送给模型的历史消息条数（最近若干条） */
    private int historyMessages = 20;

    /** 流式回答开始输出后，超过这么久没有收到新数据视为中断（关闭连接并提示超时） */
    private Duration streamIdleTimeout = Duration.ofSeconds(60);

    /** SSE 流式回答的最长时间 */
    private Duration streamTimeout = Duration.ofMinutes(5);

    /** 同时进行的流式回答数（超出时排队） */
    private int streamThreads = 16;

    /**
     * SaaS 模式下非平台租户的接口地址是否允许指向内网 / 本机地址（防止借「测试连接」探测内网），默认不允许；
     * 平台租户和单租户部署不受限制（可接入私有化部署的模型）
     */
    private boolean allowPrivateNetwork = false;

    /** 数据整理：一个任务最多读取的记录数（超出时只分析前面的，提示按关键字分批整理） */
    private int cleanseMaxRecords = 2000;

    /** 数据整理：规范化建议每次发给模型的记录数 */
    private int cleanseBatchSize = 40;

    /** 数据整理：一个任务最多交给模型判断的疑似重复组数 */
    private int cleanseMaxGroups = 100;

    /** 数据整理：疑似重复每次发给模型的组数 */
    private int cleanseGroupsPerCall = 10;

    /** 数据整理：同时执行的任务数（超出时排队） */
    private int cleanseThreads = 2;

    /** 数据整理：进行中的任务超过这么久没有进展（如服务重启）视为中断，可以重新发起 */
    private Duration cleanseStaleAfter = Duration.ofMinutes(30);
}
