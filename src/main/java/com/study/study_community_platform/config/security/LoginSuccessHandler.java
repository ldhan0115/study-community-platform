package com.study.study_community_platform.config.security;

import com.study.study_community_platform.controller.web.SessionConst;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
// 로그인 성공 후 실행되는 클래스
public class LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    public LoginSuccessHandler(){

        // 로그인 전 보호 URL이 없으면 메인 화면으로 이동
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {

        // 인증에 성공한 principal이 LoginMemberPrincipal인지 확인
        if(!(authentication.getPrincipal() instanceof LoginMemberPrincipal principal)){
            throw new IllegalStateException("지원하지 않는 인증 사용자입니다.");
        }

        // Controller와 화면에서 사용할 수 있도록 비밀번호가 없는 최소 DTO를 세션에 저장
        request.getSession().setAttribute(SessionConst.LOGIN_MEMBER, principal.toLoginMemberSession());

        // 로그인 전에 접근했던 내부 페이지가 있으면 그곳으로 이동
        // 없으면 기본 주소 "/"로 이동
        // redirectURL 요청 파라미터는 직접 사용 x
        // -> 사용자가 원하는 값을 보내 로그인 후 외부 사이트로 이동 시킬 수 있음으로
        // -> redirectURL=https://evil.example
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
