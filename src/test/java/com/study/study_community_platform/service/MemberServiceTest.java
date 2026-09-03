package com.study.study_community_platform.service;

import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.service.dto.MemberUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class MemberServiceTest {

    @Autowired MemberService memberService;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void join(){
        // given
        JoinMemberForm form = new JoinMemberForm("test", "1234", "test@gmail.com", "tester");

        // when
        Long joinedId = memberService.join(form);
        Member findMember = memberService.findMember(joinedId);

        // then
        // 회원 가입 후 조회한 회원이 동일한지 검증
        assertThat(findMember.getLoginId()).isEqualTo(form.getLoginId());
        assertThat(passwordEncoder.matches(form.getPassword(), findMember.getPassword())).isTrue();
    }

    @Test
    void validateDuplicateLoginId(){
        // given
        JoinMemberForm form1 = new JoinMemberForm("test", "1234", "test1@gmail.com", "tester1");
        JoinMemberForm form2 = new JoinMemberForm("test", "1234", "test2@gmail.com", "tester2");

        memberService.join(form1);

        // when & then
        // 동일한 loginId로 가입 시 예외가 발생해야 함
        assertThatThrownBy(() -> memberService.join(form2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateDuplicateEmail(){
        // given
        JoinMemberForm form1 = new JoinMemberForm("test1", "1234", "test@gmail.com", "tester1");
        JoinMemberForm form2 = new JoinMemberForm("test2", "1234", "test@gmail.com", "tester2");

        memberService.join(form1);

        // when & then
        // 동일한 email로 가입 시 예외가 발생해야 함
        assertThatThrownBy(() -> memberService.join(form2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateDuplicateNickname(){
        // given
        JoinMemberForm form1 = new JoinMemberForm("test1", "1234", "test1@gmail.com", "tester");
        JoinMemberForm form2 = new JoinMemberForm("test2", "1234", "test2@gmail.com", "tester");

        memberService.join(form1);

        // when & then
        // 동일한 nickname으로 가입 시 예외가 발생해야 함
        assertThatThrownBy(() -> memberService.join(form2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findMembers(){
        // given
        JoinMemberForm form1 = new JoinMemberForm("test1", "1234", "test1@gmail.com", "tester1");
        JoinMemberForm form2 = new JoinMemberForm("test2", "1234", "test2@gmail.com", "tester2");

        Long joinedId1 = memberService.join(form1);
        Long joinedId2 = memberService.join(form2);

        Member findMember1 = memberService.findMember(joinedId1);
        Member findMember2 = memberService.findMember(joinedId2);

        // when
        List<Member> members = memberService.findMembers();

        // then
        // 전체 회원 수와 포함 여부 검증
        assertThat(members.size()).isEqualTo(2);
        assertThat(members).contains(findMember1, findMember2);
    }

    @Test
    void editMemberKeepsMemberIdAndEncodedPassword(){
        // given
        JoinMemberForm joinMemberForm = new JoinMemberForm(
                "member1",
                "password123",
                "member1@gmail.com",
                "nickname1"
        );

        Long memberId = memberService.join(joinMemberForm);

        MemberUpdateDto memberUpdateDto = new MemberUpdateDto(
                "member1",
                "newPassword",
                "member1@gmail.com",
                "newNickname"
        );

        //when
        Member updatedMember = memberService.editMember(memberId, memberUpdateDto);

        //then
        assertThat(updatedMember.getId()).isEqualTo(memberId);
        assertThat(updatedMember.getNickname()).isEqualTo("newNickname");
        assertThat(passwordEncoder.matches("newPassword", updatedMember.getPassword())).isTrue();
        assertThat(updatedMember.getPassword()).isNotEqualTo("newPassword");
    }
}