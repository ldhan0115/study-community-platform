package com.study.study_community_platform.config;

import com.study.study_community_platform.controller.web.argumentresolver.LoginMemberArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // Controller의 @Login 파라미터 처리
        resolvers.add(new LoginMemberArgumentResolver());
    }
}
