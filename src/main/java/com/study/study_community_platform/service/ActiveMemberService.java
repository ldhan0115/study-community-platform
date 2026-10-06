package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.exception.ForbiddenOperationException;
import com.study.study_community_platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActiveMemberService {

    private final MemberRepository memberRepository;

    // 필터에서 현재 로그인 회원의 활성 여부를 확인할 때 사용
    public boolean isActive(Long memberId){
        return memberId != null
                && memberRepository.existsByIdAndDeletedAtIsNull(memberId);
    }

    // 서비스의 쓰기 작업에 사용할 활성 회원을 조회
    public Member requireActive(Long memberId){
        return memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new ForbiddenOperationException(
                        "존재하지 않거나 탈퇴한 회원은 작업할 수 없습니다."
                ));
    }
}
