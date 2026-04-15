package com.ly.springquickstart.interceptor;

import com.ly.springquickstart.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头（Header）中拿到那个叫 Authorization 的手环（Token）
        String token = request.getHeader("Authorization");

        try {
            // 2. 把手环扔给我们的机器去验真伪
            Map<String, Object> claims = JwtUtils.parseToken(token);

            // 3. 验真成功，保安放行！(return true)
            return true;
        } catch (Exception e) {
            // 4. 如果手环是假的、过期的，或者压根没带手环，就会走到这里
            response.setStatus(401); // 401 在 HTTP 状态码里代表“未认证/未登录”
            return false; // 保安无情拦截！
        }
    }
}