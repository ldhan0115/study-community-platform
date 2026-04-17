package com.study.study_community_platform.controller.web.interceptor;

import com.study.study_community_platform.controller.web.SessionConst;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class LoginCheckInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();

        // 미인증 사용자에게 불필요한 세션이 생성되는 것을 방지하기 위해 false 설정
        HttpSession session = request.getSession(false);

        if(session == null || session.getAttribute(SessionConst.LOGIN_MEMBER) == null){
            // 로그인 성공 후 기존 페이지로 돌려보내기 위해 redirectURL 파라미터 포함
            response.sendRedirect("/members/login?redirectURL=" + requestURI);
            return false;
        }

        return true;
    }
}