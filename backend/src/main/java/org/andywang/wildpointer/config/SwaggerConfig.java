package org.andywang.wildpointer.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Wild Pointer API")
                        .version("0.5.0")
                        .description("野指针 - 随机探索路线生成器 API 文档"));
    }
}
