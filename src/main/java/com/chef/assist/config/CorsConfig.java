package com.chef.assist.config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
@Slf4j
@Configuration
public class CorsConfig {

//    @Bean
//    public CorsFilter corsFilter() {
//        log.info("init cors");
//        CorsConfiguration config = new CorsConfiguration();
//        config.addAllowedOrigin("*");  // 允许所有域访问
//        config.addAllowedHeader("*");  // 允许所有请求头
//        config.addAllowedMethod("*");  // 允许所有HTTP方法
//        config.setAllowCredentials(true);  // 允许发送身份验证信息（如Cookies）
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("api/v1/**", config);  // 对所有URL生效
//        return new CorsFilter(source);
//    }
}
