package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.service.MemberService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
// 서버를 별도로 실행하지 않고 HTTP 요청을 테스트
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class SecurityAuthenticationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberService memberService;

    @Test
    void activeMemberCanLogin() throws Exception {
        Long memberId = joinMember("activeMember", "active@test.com", "활성 회원");

        mockMvc.perform(
                post("/members/login")
                        // 정상적인 CSRF 토큰 추가
                        .with(csrf())

                        // 로그인 폼에 입력하는 값
                        .param(
                                "loginId",
                                "activeMember"
                        )
                        .param("password",
                                "password123")
        )

                // 로그인 성공 후 리다이렉트
                .andExpect(status().is3xxRedirection())
                // 이전 요청이 없으므로 홈 화면으로 이동
                .andExpect(redirectedUrl("/"))
                // Spring Security 인증 성공 확인
                .andExpect(
                        authenticated().withUsername("activeMember")
                )
                // 화면용 최소 DTO가 세션에 저장됐는지 확인
                .andExpect(
                        request().sessionAttribute(
                                SessionConst.LOGIN_MEMBER,
                                new LoginMemberSession(
                                        memberId,
                                        "활성 회원"
                                )
                        )
                );
    }

    @Test
    void withdrawnMemberCannotLogin() throws Exception {
        Long memberId = joinMember("withdrawn", "withdrawn@test.com", "탈퇴 회원");

        memberService.withdrawMember(memberId);

        mockMvc.perform(
                post("/members/login")
                        .with(csrf())
                        .param("loginId",
                                "withdrawn")
                        .param("password",
                                "password123")
        )

                // 로그인 실패 후 로그인 화면으로 리다이렉트
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/members/login?error")
                )
                // SecurityContext에 인증이 저장 x
                .andExpect(unauthenticated());
    }

    @Test
    void loginWithoutCsrfTokenIsRejected() throws Exception {
        joinMember("noCsrf", "noCsrf@test.com", "CSRF");

        mockMvc.perform(
                post("/members/login")
                        .param("loginId", "noCsrf")
                        .param("password", "password123")
                )

                // 인증 처리 전에 CSRF Filter가 차단 (403)
                .andExpect(status().isForbidden())
                .andExpect(unauthenticated());
    }

    @Test
    void externalRedirectParameterIsIgnored() throws Exception {
        joinMember("safeRedirect", "redirect@test.com", "리다이렉트");

        mockMvc.perform(
                post("/members/login")
                        .with(csrf())
                        .param("loginId", "safeRedirect")
                        .param("password", "password123")

                        // 악의적인 외부 이동 주소를 전달
                        .param("redirectURL", "https://evil.example")
        )
                // 로그인 자체는 정상적으로 성공
                .andExpect(status().is3xxRedirection())
                // 외부 사이트가 아니라 안전한 기본 주소로 이동
                .andExpect(redirectedUrl("/"))
                // 외부 주소를 무시했을 뿐 로그인은 성공해야함
                .andExpect(authenticated());
    }

    @Test
    void sessionIdChangesAfterLogin() throws Exception {
        joinMember("sessionMember", "session@test.com", "세션");

        // 로그인 전 세션 생성 후  ID 저장
        MockHttpSession session = new MockHttpSession();
        String oldSessionId = session.getId();

        mockMvc.perform(
                post("/members/login")

                        // 만들어 둔 세션으로 로그인 요청
                        .session(session)
                        .with(csrf())
                        .param("loginId", "sessionMember")
                        .param("password", "password123")
        )
                .andExpect(status().is3xxRedirection())
                .andExpect(authenticated());


        // 로그인 전후 세션 ID가 달라야함
        // 세션 내부의 필요한 데이터는 유지하되 세션 식별자만 새로 발급
        assertThat(session.getId())
                .isNotEqualTo(oldSessionId);
    }


    private Long joinMember(String loginId, String email, String nickname){
        return memberService.join(
                new JoinMemberForm(
                        loginId,
                        "password123",
                        email,
                        nickname
                )
        );
    }
}
