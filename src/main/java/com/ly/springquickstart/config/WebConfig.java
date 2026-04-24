package com.ly.springquickstart.config;

import com.ly.springquickstart.interceptor.LoginInterceptor;
import com.ly.springquickstart.interceptor.RateLimitInterceptor;
import com.ly.springquickstart.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired private LoginInterceptor     loginInterceptor;
    @Autowired private RoleInterceptor      roleInterceptor;
    @Autowired private RateLimitInterceptor rateLimitInterceptor;

    @Value("${app.upload-dir:#{null}}")
    private String uploadDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/login", "/register", "/orders", "/orders/pay");

        registry.addInterceptor(loginInterceptor)
                .excludePathPatterns(
                        "/doc.html", "/v3/api-docs/**",
                        "/login", "/register", "/error",
                        "/ai/chat",
                        "/items/search",
                        "/items/page",
                        "/items/{id}",
                        "/categories/tree",
                        "/reviews/items/**",
                        "/coupons/available",
                        "/uploads/**",
                        "/ws/**"
                );

        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absPath;
        if (uploadDir != null && !uploadDir.isBlank()) {
            absPath = Paths.get(uploadDir).toAbsolutePath().toString();
        } else {
            absPath = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().toString();
        }
        String loc = "file:" + absPath.replace("\\", "/") + "/";
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(loc);
    }
}
