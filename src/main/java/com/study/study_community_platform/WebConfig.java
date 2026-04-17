package com.study.study_community_platform;

import com.study.study_community_platform.controller.web.argumentresolver.LoginMemberArgumentResolver;
import com.study.study_community_platform.controller.web.interceptor.LoginCheckInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new LoginMemberArgumentResolver());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LoginCheckInterceptor())
                .order(1) // interceptor 처리 순서
                .addPathPatterns("/**") // 기본으로 모든 경로 검사
                .excludePathPatterns("/", "/members/join", "/members/login", "/members/logout",
                        "/css/**", "/vendor/**", "/error"); // 검사 예외 경로
    }
}
