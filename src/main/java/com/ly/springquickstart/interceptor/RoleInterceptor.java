package com.ly.springquickstart.interceptor;

import com.ly.springquickstart.annotation.RoleRequired;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equals(request.getMethod())) return true;
        if (!(handler instanceof HandlerMethod hm)) return true;

        RoleRequired ann = hm.getMethodAnnotation(RoleRequired.class);
        if (ann == null) return true;

        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null || claims.get("role") == null) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"无权访问\"}");
            return false;
        }

        int userRole = Integer.parseInt(claims.get("role").toString());
        // 管理员 (role=2) 是超级角色，拥有所有权限
        if (userRole == 2) return true;
        for (int allowed : ann.value()) {
            if (userRole == allowed) return true;
        }

        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":0,\"msg\":\"权限不足\"}");
        return false;
    }
}
