package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;

    // 회원 가입
    @Transactional
    public Long join(Member member){
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    // 회원 단건 조회
    public Member findMember(Long memberId){
        Optional<Member> byId = memberRepository.findById(memberId);
        return byId.orElse(null); // 해당 회원 없으면 null 반환
    }

    // 전체 회원 조회
    public List<Member> findMembers(){
        return memberRepository.findAll();
    }

    // loginId, email, nickname 중복 여부 검증
    private void validateDuplicateMember(Member member) {
        List<Member> byLoginId = memberRepository.findByLoginId(member.getLoginId());
        if(!byLoginId.isEmpty()){
            throw new IllegalStateException("동일한 ID가 존재합니다.");
        }

        List<Member> byEmail = memberRepository.findByEmail(member.getEmail());
        if(!byEmail.isEmpty()){
            throw new IllegalStateException("동일한 EMAIL이 존재합니다.");
        }

        List<Member> byNickname = memberRepository.findByNickname(member.getNickname());
        if(!byNickname.isEmpty()){
            throw new IllegalStateException("동일한 NICKNAME이 존재합니다.");
        }
    }
}