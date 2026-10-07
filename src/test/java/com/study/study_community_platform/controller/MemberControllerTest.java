package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.member.EditMemberForm;
import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MemberControllerTest {

    @Autowired
    MemberController memberController;
    @Autowired
    MemberService memberService;

    @Test
    void editKeepsLoginMemberIdAndUpdatedNickname() {
        // given
        Long memberId = memberService.join(
                new JoinMemberForm(
                        "member1",
                        "password123",
                        "member1@test.com",
                        "nickname1"
                )
        );

        Member member = memberService.findMember(memberId);
        LoginMemberSession loginMember = LoginMemberSession.from(member);

        EditMemberForm editForm = new EditMemberForm();
        editForm.setLoginId("member1");
        editForm.setPassword("newPassword");
        editForm.setEmail("member1@test.com");
        editForm.setNickname("newNickname");

        BindingResult bindingResult = new BeanPropertyBindingResult(editForm, "member");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpSession session = new MockHttpSession();

        request.setSession(session);
        session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);

        //when
        String viewName = memberController.edit(editForm, bindingResult, loginMember, request, response);

        Member updated = memberService.findMember(memberId);

        //then

        // 회원을 새로 생성하지 않고 기존 회원의 정보를 수정
        assertThat(updated.getId()).isEqualTo(memberId);
        assertThat(updated.getNickname()).isEqualTo("newNickname");

        // 수정 성공 후 현재 세션을 종료하고 재로그인 요청
        assertThat(session.isInvalid()).isTrue();
        assertThat(viewName).isEqualTo("redirect:/members/login?updated");

    }

    @Test
    void loginSessionDoesNotContainPassword() {
        // given
        String[] fieldNames = Arrays.stream(LoginMemberSession.class.getRecordComponents())
                .map(RecordComponent::getName)
                .toArray(String[]::new);

        assertThat(fieldNames).containsExactly("id", "nickname");
        assertThat(fieldNames).doesNotContain("password");

        //when

        //then
    }
}
