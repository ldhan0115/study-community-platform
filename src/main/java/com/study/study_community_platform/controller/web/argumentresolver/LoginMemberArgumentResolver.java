package com.study.study_community_platform.controller.web.argumentresolver;

import com.study.study_community_platform.config.security.LoginMemberPrincipal;
import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Objects;

public class LoginMemberArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {

        boolean hasLoginAnnotation = parameter.hasParameterAnnotation(Login.class);
        boolean hasLoginMemberSessionType = LoginMemberSession.class.isAssignableFrom(parameter.getParameterType());

        // @Login이 붙어 있고 LoginMemberSession 타입인 매개변수만 처리
        return hasLoginAnnotation && hasLoginMemberSessionType;
    }

    @Override
    public @Nullable Object resolveArgument(MethodParameter parameter,
                                            @Nullable ModelAndViewContainer mavContainer,
                                            NativeWebRequest webRequest,
                                            @Nullable WebDataBinderFactory binderFactory) throws Exception {

        // 현재 요청의 Spring Security 인증 정보 조회
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        /*
        다음 경우에는 로그인 회원 x

        1. Authentication이 없음
        2. 인증되지 않음
        3. 익명 사용자
        4. Principal 타입이 아님
         */
        if(authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !(authentication.getPrincipal() instanceof LoginMemberPrincipal principal)){
            return null;
        }

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        HttpSession session = request == null ? null : request.getSession(false);

        // 세션이 존재할 시 Security가 인증한 회원 ID와 같을 때만 해당 세션 신뢰
        if(session != null && session.getAttribute(
                SessionConst.LOGIN_MEMBER
        ) instanceof LoginMemberSession loginMember &&
                Objects.equals(loginMember.id(), principal.getMemberId())){
            return loginMember;
        }

        // 세션이 없으면 인증된 principal을 세션 DTO로 변환
        return principal.toLoginMemberSession();
    }
}
