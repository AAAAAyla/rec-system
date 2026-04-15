package com.ly.springquickstart.config;

import com.ly.springquickstart.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // 增加针对 Knife4j 和 Swagger 资源的放行
                .excludePathPatterns(
                        "/users/login",
                        "/users/register",
                        "/doc.html",            // Knife4j 页面
                        "/v3/api-docs/**",      // OpenAPI 结构化数据
                        "/webjars/**",           // 前端静态资源
                        "/items/**"
                );
    }
    /**
     *  跨域 (CORS) 全局配置
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 1. 允许哪些后端的接口被跨域访问？("/**" 代表所有接口)
        registry.addMapping("/**")

                // 2. 允许哪些前端的网址来访问？(比如只允许 http://localhost:3000)
                // 面试考点：在老版本是用 allowedOrigins("*")，但在包含鉴权的现代项目中，
                // 推荐使用 allowedOriginPatterns("*")，它更安全且支持通配符。
                .allowedOriginPatterns("*")

                // 3. 允许前端使用哪些 HTTP 方法？
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")

                // 4. 允许前端携带哪些请求头？(比如咱们的 Authorization 手环)
                .allowedHeaders("*")

                // 5. 是否允许前端携带 Cookie 等凭证信息？(重要！如果前端需要传 Token，这里必须是 true)
                .allowCredentials(true)

                // 6. 预检请求(OPTIONS)的缓存时间，单位是秒。
                // 浏览器在发复杂请求前，会先发一个 OPTIONS 请求问问后端行不行。
                // 设置 3600 秒(1小时)缓存，意味着1小时内前端不用每次都发两遍请求，极大提升性能！
                .maxAge(3600);
    }
}