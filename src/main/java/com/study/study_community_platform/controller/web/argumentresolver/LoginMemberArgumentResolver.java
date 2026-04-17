package com.study.study_community_platform.controller.web.argumentresolver;

import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.domain.Member;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        boolean hasLoginAnnotation = parameter.hasParameterAnnotation(Login.class);
        boolean hasMemberType = Member.class.isAssignableFrom(parameter.getParameterType());

        // Login 어노테이션이 붙어있는지 && Member 타입 객체인지 확인해 해당하는 객체만 지원
        return hasLoginAnnotation && hasMemberType;
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer, NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest request = (HttpServletRequest)webRequest.getNativeRequest();
        HttpSession session = request.getSession(false);
        if(session == null){
            // 세션 정보 없으면 널 반환
            return null;
        }

        // 해당하는 객체 있으면 주입
        return session.getAttribute(SessionConst.LOGIN_MEMBER);
    }
}
