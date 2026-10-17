package com.lemon.erp.framework.demo;

import com.lemon.erp.common.i18n.I18n;
import com.lemon.erp.common.result.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 演示模式：拦截除 GET 以外的请求（白名单除外），返回提示而不执行。
 * 在 @PreAuthorize 和业务代码之前执行；返回 HTTP 200 + 业务码 403，前端直接提示 msg
 */
@RequiredArgsConstructor
public class DemoModeInterceptor implements HandlerInterceptor {

    public static final String MESSAGE = "演示模式下不能新增、修改、删除数据";

    private static final Set<String> READ_METHODS = Set.of(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.OPTIONS.name());

    private final DemoProperties properties;
    private final PathMatcher matcher = new AntPathMatcher();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!properties.isEnabled() || READ_METHODS.contains(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (properties.getAllowedPaths().stream().anyMatch(p -> matcher.match(p, path))) {
            return true;
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"code\":" + R.FORBIDDEN + ",\"msg\":\"" + I18n.t(MESSAGE) + "\"}");
        return false;
    }
}
