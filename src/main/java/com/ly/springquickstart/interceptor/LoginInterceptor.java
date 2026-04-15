package com.ly.springquickstart.interceptor;

import com.ly.springquickstart.utils.JwtUtils;
import com.ly.springquickstart.utils.ThreadLocalUtil; // 引入刚刚写的储物柜工具
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    // 这个方法在请求到达 Controller 之前执行（查票）
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("Authorization");
        try {
            // 1. 验证手环
            Map<String, Object> claims = JwtUtils.parseToken(token);

            // 2. ⭐ 新增逻辑：验证通过后，把从手环里解析出的用户信息，存入 ThreadLocal（储物柜）
            ThreadLocalUtil.set(claims);

            return true; // 放行
        } catch (Exception e) {
            response.setStatus(401);
            return false; // 拦截
        }
    }

    // 这个方法在整个请求处理完毕，准备返回给前端之后执行（善后工作）
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // ⭐ 新增逻辑：必须清空当前线程的储物柜，防止内存泄漏！
        ThreadLocalUtil.remove();
    }
}