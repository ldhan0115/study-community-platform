package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class MemberServiceTest {

    @Autowired MemberService memberService;

    @Test
    void join(){
        // given
        Member member = new Member("test", "1234", "test@gmail.com", "tester");

        // when
        Long joinedId = memberService.join(member);
        Member findMember = memberService.findOne(joinedId);

        // then
        // 회원 가입 후 조회한 회원이 동일한지 검증
        assertThat(findMember).isEqualTo(member);
    }

    @Test
    void validateDuplicateLoginId(){
        // given
        Member member1 = new Member("test", "1234", "test1@gmail.com", "tester1");
        Member member2 = new Member("test", "1234", "test2@gmail.com", "tester2");

        memberService.join(member1);

        // when & then
        // 동일한 loginId로 가입 시 예외가 발생해야 함
        assertThatThrownBy(() -> memberService.join(member2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateDuplicateEmail(){
        // given
        Member member1 = new Member("test1", "1234", "test@gmail.com", "tester1");
        Member member2 = new Member("test2", "1234", "test@gmail.com", "tester2");

        memberService.join(member1);

        // when & then
        // 동일한 email로 가입 시 예외가 발생해야 함
        assertThatThrownBy(() -> memberService.join(member2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateDuplicateNickname(){
        // given
        Member member1 = new Member("1test", "1234", "test1@gmail.com", "tester");
        Member member2 = new Member("2test", "1234", "test2@gmail.com", "tester");

        memberService.join(member1);

        // when & then
        // 동일한 nickname으로 가입 시 예외가 발생해야 함
        assertThatThrownBy(() -> memberService.join(member2))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void findMembers(){
        // given
        Member member1 = new Member("1test", "1234", "test1@gmail.com", "tester1");
        Member member2 = new Member("2test", "1234", "test2@gmail.com", "tester2");

        memberService.join(member1);
        memberService.join(member2);

        // when
        List<Member> members = memberService.findMembers();

        // then
        // 전체 회원 수와 포함 여부 검증
        assertThat(members.size()).isEqualTo(2);
        assertThat(members).contains(member1, member2);
    }
}