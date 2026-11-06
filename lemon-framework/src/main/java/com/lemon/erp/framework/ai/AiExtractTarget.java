package com.lemon.erp.framework.ai;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * 智能录入的目标单据 / 资料：把上传的图片、PDF、Excel、文本识别成单据草稿。业务模块实现为 Spring Bean，AI 模块收集。
 * <p>
 * 流程：用户选择目标（只列出有 {@link #permission()} 的）并上传文件 → AI 模块让模型按 {@link #schema()} 输出 JSON
 * （JSON 模式，校验不通过时重试）→ 调用 {@link #prefill(Map)} 把名称匹配成 ID → 前端打开 {@link #route()} 的
 * {@link #form()} 表单预填，用户核对后自己保存。
 *
 * <p><b>实现约定</b>
 * <ul>
 *   <li>{@link #schema()} 只描述单据上能看到的信息（供应商名称、日期、物料名称 / 规格、数量、单价等），不要求模型给出 ID</li>
 *   <li>{@link #prefill(Map)} 只查询、不写库；名称匹配用本模块 Service（按编码精确、名称精确、名称包含的顺序），
 *       匹配不上或匹配到多个时保留原文并加一条 {@link AiPrefill.Warning}</li>
 * </ul>
 */
public interface AiExtractTarget {

    /** 标识的格式：模块.名称，小写字母、数字、连字符，如 {@code scm.purchase-order} */
    Pattern KEY_PATTERN = Pattern.compile("^[a-z]+\\.[a-z0-9-]+$");

    /** 全局唯一的标识，如 {@code scm.purchase-order} */
    String key();

    /** 名称的消息键（如「采购订单」），显示在目标列表中 */
    String labelKey();

    /** 使用该目标所需的权限，一般是对应单据的新增权限（如 {@code scm:purchase:add}）；为 null 表示登录即可 */
    String permission();

    /** 模型输出的 JSON Schema（type 为 object），可用 {@link AiSchema} 构造，说明文字按当前用户语言生成 */
    Map<String, Object> schema();

    /** 给模型的补充说明的消息键（如「金额为含税金额」「日期格式」），没有时为 null */
    default String instructionKey() {
        return null;
    }

    /**
     * 把模型输出（已按 {@link #schema()} 校验）转换为前端表单的预填数据
     *
     * @param extracted 模型输出的 JSON 对象（值为 String / Number / Boolean / List / Map）
     */
    AiPrefill prefill(Map<String, Object> extracted);

    /** 前端打开的页面路由，如 {@code /scm/purchase} */
    String route();

    /** 前端表单标识：页面据此打开对应的新增表单并预填，如 {@code purchase-order} */
    String form();
}
