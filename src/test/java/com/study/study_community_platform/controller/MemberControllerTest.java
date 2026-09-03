package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.member.EditMemberForm;
import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.service.MemberService;
import jakarta.servlet.http.HttpSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class MemberControllerTest {

    @Autowired MemberController memberController;
    @Autowired MemberService memberService;

    @Test
    void editKeepsLoginMemberIdAndUpdatedNickname(){
        // given
        JoinMemberForm joinForm = new JoinMemberForm(
                "member1",
                "password123",
                "member1@test.com",
                "nickname1"
        );

        Long memberId = memberService.join(joinForm);
        Member member = memberService.findMember(memberId);

        LoginMemberSession loginMember = LoginMemberSession.from(member);

        EditMemberForm editForm = new EditMemberForm();
        editForm.setLoginId("member1");
        editForm.setPassword("newPassword");
        editForm.setEmail("member1@test.com");
        editForm.setNickname("newNickname");

        BindingResult bindingResult = new BeanPropertyBindingResult(editForm, "member");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession().setAttribute(SessionConst.LOGIN_MEMBER, loginMember);

        //when
        String viewName = memberController.edit(editForm, bindingResult, loginMember, request);

        HttpSession session = request.getSession(false);

        LoginMemberSession updatedSessionMember = (LoginMemberSession) session.getAttribute(SessionConst.LOGIN_MEMBER);

        //then
        assertThat(viewName).isEqualTo("redirect:/");
        assertThat(updatedSessionMember.id()).isEqualTo(memberId);
        assertThat(updatedSessionMember.nickname()).isEqualTo("newNickname");

    }

    @Test
    void loginSessionDoesNotContainPassword(){
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
