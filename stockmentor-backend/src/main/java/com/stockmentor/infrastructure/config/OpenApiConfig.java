package com.stockmentor.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    OpenAPI stockMentorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("StockMentor API")
                        .version("v1")
                        .description("股票与投资学习系统 API，仅用于投资教育和虚拟学习。"));
    }
}
