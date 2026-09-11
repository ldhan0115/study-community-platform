package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthenticationAccessTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    StudyRepository studyRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/members/join",
            "/members/login",
            "/studies"
    })
    void guestCanAccessPublicPages(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isOk());
    }

    @Test
    void guestCanAccessStudyDetail() throws Exception {
        Member member = saveMember();

        Study study = studyRepository.save(
                Study.createStudy(
                        member,
                        "공개 상세 조회 테스트",
                        "비로그인 사용자도 조회 가능",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );

        mockMvc.perform(get("/studies/{studyId}", study.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("studies/studyDetail"));
    }

    private Member saveMember(){
        return memberRepository.save(
                Member.createMember(
                        "securityMember",
                        "password123",
                        "security@test.com",
                        "보안 테스트"
                )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/members/edit",
            "/studies/new",
            "/studies/1/edit",
            "/studies/1/applicants",
            "/applications"
    })
    void guestIsRedirectedFromProtectedPages(String path) throws Exception{
        mockMvc.perform(get(path))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                "/members/login?redirectURL=" + path
                        )
                );
    }

    @Test
    void loggedInMemberCanAccessProtectedPage() throws Exception {

        Member member = saveMember();
        LoginMemberSession loginMember = LoginMemberSession.from(member);

        mockMvc.perform(
                get("/members/edit")
                        .sessionAttr(
                                SessionConst.LOGIN_MEMBER,
                                loginMember
                        )
        )
                .andExpect(status().isOk())
                .andExpect(view().name("members/editMemberForm"))
                .andExpect(model().attributeExists("member"));
    }




}
