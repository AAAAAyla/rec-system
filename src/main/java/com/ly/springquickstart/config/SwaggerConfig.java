package com.ly.springquickstart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("亚马逊推荐系统 API 文档")
                        .description("包含用户鉴权、数据读取与算法结果写入功能")
                        .version("v1.0"));
    }
}