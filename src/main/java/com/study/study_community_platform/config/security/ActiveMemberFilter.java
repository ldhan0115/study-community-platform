package com.study.study_community_platform.config.security;

import com.study.study_community_platform.service.ActiveMemberService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class ActiveMemberFilter extends OncePerRequestFilter {

    private final ActiveMemberService activeMemberService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if(authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal()
                    instanceof LoginMemberPrincipal principal
                && !activeMemberService.isActive(principal.getMemberId())){

            // 세션과 SecurityContext를 함께 정리
            logoutHandler.logout(request, response, authentication);

            response.sendRedirect(
                    request.getContextPath() + "/members/login?withdrawn"
            );

            // 탈퇴 회원의 요청이 Controller까지 진행되지 않도록 종료
            return;
        }

        filterChain.doFilter(request, response);

    }
}
