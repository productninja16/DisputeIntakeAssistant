package com.disputeintake.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
class WebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AnalystAccessInterceptor())
                .addPathPatterns("/analyst/**")
                .excludePathPatterns("/analyst", "/analyst/sign-in", "/analyst/sign-out");
    }
}
