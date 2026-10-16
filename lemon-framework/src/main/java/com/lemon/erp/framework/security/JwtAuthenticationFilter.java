package com.lemon.erp.framework.security;

import com.lemon.erp.common.result.R;
import com.lemon.erp.framework.portal.PortalFeatures;
import com.lemon.erp.framework.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 从 Authorization: Bearer xxx 中解析 token，校验服务端会话仍然存在（未注销/未被强退），
 * 再加载用户权限（带缓存）放入 SecurityContext，并设置当前租户（请求结束后清除）
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Long previous = TenantContext.get();
        try {
            LoginUser user = authenticate(request);
            String denied = user == null ? null
                    : PortalFeatures.deniedMessage(user, request.getRequestURI().substring(request.getContextPath().length()));
            if (denied != null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":" + R.FORBIDDEN + ",\"msg\":\"" + denied + "\"}");
                return;
            }
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            // 一般为 null；在测试等已设置租户的线程中直接调用时恢复原租户
            if (previous != null) {
                TenantContext.set(previous);
            }
        }
    }

    private LoginUser authenticate(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)) {
            OnlineSession session = tokenService.getSession(header.substring(BEARER.length()));
            // 升级前创建的会话没有租户，视为失效，重新登录
            if (session != null && session.tenantId() == null) {
                session = null;
            }
            // 先按会话设置租户，加载用户权限的查询才能带上租户条件
            if (session != null) {
                TenantContext.set(session.tenantId());
            }
            LoginUser loginUser = session == null ? null : tokenService.getLoginUser(session.userId());
            if (loginUser != null) {
                loginUser = loginUser.withTokenId(session.tokenId());
                List<SimpleGrantedAuthority> authorities = loginUser.perms().stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
                var authentication = new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            return loginUser;
        }
        return null;
    }
}
