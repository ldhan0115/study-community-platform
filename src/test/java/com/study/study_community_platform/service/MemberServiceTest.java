package com.study.study_community_platform.service;

import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.service.dto.MemberUpdateDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
@Profile("test")
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
    
    @Test
    void withdrawnMemberCannotLogin(){
        // given
        JoinMemberForm joinForm = new JoinMemberForm("withdrawnMember", "1234",
                "withdraw@test.com", "탈퇴회원");

        Long memberId = memberService.join(joinForm);

        Member member = memberService.findMember(memberId);

        memberService.withdrawMember(memberId);

        //when
        Member loginMember = memberService.login("withdrawnMember", "1234");

        //then
        assertThat(member.getDeletedAt()).isNotNull();
        assertThat(loginMember).isNull();
    }

    @Test
    void withdrawnMemberCannotBeFound(){
        // given
        JoinMemberForm joinForm = new JoinMemberForm("withdrawn", "1234",
                "withdrawn@test.com", "탈퇴 회원");

        Long memberId = memberService.join(joinForm);
        memberService.withdrawMember(memberId);

        //when & then
        assertThatThrownBy(() ->
                memberService.findMember(memberId)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessage("존재하지 않거나 탈퇴한 회원입니다.");
    }

    @Test
    void activeMemberCanLogin(){
        // given
        JoinMemberForm joinForm = new JoinMemberForm("join", "1234",
                "join@test.com", "가입 회원");

        Long memberId = memberService.join(joinForm);

        //when
        Member loginMember = memberService.login("join", "1234");

        //then
        assertThat(loginMember).isNotNull();
        assertThat(loginMember.getId()).isEqualTo(memberId);
    }

    @Test
    void withdrawnMemberCannotRejoinWithSameLoginId(){
        // given
        JoinMemberForm firstJoinForm = new JoinMemberForm("first", "1234",
                "first@test.com", "처음 가입");

        Long memberId = memberService.join(firstJoinForm);
        memberService.withdrawMember(memberId);

        JoinMemberForm secondJoinForm = new JoinMemberForm("first", "1234",
                "second@test.com", "다시 가입");

        //when & then
        assertThatThrownBy(() ->
                memberService.join(secondJoinForm)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessage("동일한 ID가 존재합니다.");

    }
}