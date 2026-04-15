package com.ly.springquickstart.config;

import com.ly.springquickstart.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 让保安去守所有的门 (/**)
        registry.addInterceptor(loginInterceptor)
                // 但是！登录和注册接口绝对不能拦，不然别人连买票的机会都没了
                .excludePathPatterns("/users/login", "/users/register");
    }
}