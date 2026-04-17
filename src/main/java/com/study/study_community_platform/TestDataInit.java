package com.study.study_community_platform;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TestDataInit {

    private final MemberRepository memberRepository;

    @EventListener(ApplicationReadyEvent.class) // 컨테이너 준비 완료 후 실행
    @Transactional // 트랜잭션 보장
    public void init(){

        Member member = Member.createMember("test", "test1234", "test@example.com", "tester");
        memberRepository.save(member);

    }
}


