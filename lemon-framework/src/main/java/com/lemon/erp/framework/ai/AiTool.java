package com.lemon.erp.framework.ai;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * AI 问数可以调用的查询（函数调用 / tool calling）。业务模块实现为 Spring Bean，AI 模块启动时收集。
 * <p>
 * 对话时 AI 模块只把<b>当前用户有权限</b>的工具（{@link #permission()}）提供给模型；模型请求调用时，先按
 * {@link #parameters()} 校验参数，再在当前用户的登录、租户、语言上下文中调用 {@link #execute(AiToolArgs)}，
 * 结果序列化为 JSON 回传给模型，并以表格显示给用户。
 *
 * <p><b>实现约定</b>
 * <ul>
 *   <li>只做<b>查询</b>，不修改任何数据；调用本模块 Service 的查询方法，数据权限（{@code @DataPermission}）自动生效</li>
 *   <li>结果为 record / List / Map 等可序列化对象，或 {@link AiToolResult}；列表最多 {@link #MAX_ROWS} 行（超出由 AI 模块截断并告知模型），
 *       字段名用有含义的英文驼峰（模型据此理解），金额字段注明币种单位（如 {@code amountYuan}）或在说明中写明</li>
 *   <li>不返回敏感字段（密码、身份证号、银行卡号、薪资明细等），这些数据会发送给第三方模型服务</li>
 *   <li>参数错误、无数据等情况抛出 {@code BizException}，错误信息会回传给模型</li>
 * </ul>
 *
 * <pre>{@code
 * @Component
 * @RequiredArgsConstructor
 * public class SalesSummaryTool implements AiTool {
 *     private final ScmSalesReportService reportService;
 *
 *     public String name() { return "sales_summary"; }
 *     public String labelKey() { return "scm.ai.tool.salesSummary"; }
 *     public String descriptionKey() { return "scm.ai.tool.salesSummary.desc"; }
 *     public String permission() { return "scm:sales:list"; }
 *     public Map<String, Object> parameters() {
 *         return AiSchema.object()
 *                 .prop("startDate", AiSchema.date("scm.ai.param.startDate"), true)
 *                 .prop("endDate", AiSchema.date("scm.ai.param.endDate"), true)
 *                 .build();
 *     }
 *     public Object execute(AiToolArgs args) {
 *         return AiToolResult.rows(reportService.summary(args.getDate("startDate"), args.getDate("endDate")));
 *     }
 * }
 * }</pre>
 */
public interface AiTool {

    /** 单次结果的最大行数，超出部分截断 */
    int MAX_ROWS = 200;

    /** 工具名称的格式：小写字母开头，小写字母、数字、下划线，最长 64 个字符 */
    Pattern NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    /** 工具名称（全局唯一，英文小写下划线，如 {@code sales_summary}），模型按名称调用 */
    String name();

    /** 显示给用户的简短名称的消息键（如「销售订单汇总」），对话中显示为「正在查询：销售订单汇总」 */
    String labelKey();

    /** 给模型看的工具说明的消息键：做什么、返回什么、何时使用，按当前用户语言取文字 */
    String descriptionKey();

    /**
     * 参数的 JSON Schema（type 为 object），可用 {@link AiSchema} 构造；没有参数时返回 {@code AiSchema.object().build()}。
     * 每次对话时调用，参数说明按当前用户语言生成
     */
    Map<String, Object> parameters();

    /**
     * 调用所需的权限标识（{@code PermissionService#hasPerm} 语义，如 {@code scm:sales:list}）；
     * 没有该权限的用户看不到这个工具（不提供给模型）。为 null 表示登录即可
     */
    String permission();

    /**
     * 执行查询
     *
     * @param args 已按 {@link #parameters()} 校验过的参数
     * @return 可序列化为 JSON 的结果，或 {@link AiToolResult}
     */
    Object execute(AiToolArgs args);
}
