package com.ly.springquickstart.interceptor;

import com.ly.springquickstart.utils.JwtUtils;
import com.ly.springquickstart.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 直接放行浏览器的 OPTIONS 预检跨域请求
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 2. 获取请求头中的 token
        String token = request.getHeader("Authorization");

        try {
            // 3. 解析 token
            Map<String, Object> claims = JwtUtils.parseToken(token);
            // 4. 把解析后的数据存入 ThreadLocal
            ThreadLocalUtil.set(claims);
            return true;
        } catch (Exception e) {
            // 5. 解析失败，返回 401 状态码
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求完成后清除 ThreadLocal，防止内存泄漏
        ThreadLocalUtil.remove();
    }
}