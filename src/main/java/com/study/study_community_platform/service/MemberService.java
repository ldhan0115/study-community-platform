package com.study.study_community_platform.service;

import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 회원 가입
    @Transactional
    public Long join(JoinMemberForm form){
        // 입력받은 비밀번호 암호화해서 member 객체 생성
        String encodedPassword = passwordEncoder.encode(form.getPassword());
        Member member = Member.createMember(form.getLoginId(), encodedPassword, form.getEmail(), form.getNickname());

        // 회원가입 전에 loginId, email, nickname 중복 여부 검사
        validateDuplicateMember(member);
        memberRepository.save(member);
        return member.getId();
    }

    // 회원 단건 조회
    public Member findMember(Long memberId){
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));
    }

    // 전체 회원 조회
    public List<Member> findMembers(){
        return memberRepository.findAll();
    }

    // 회원 로그인
    public Member login(String loginId, String rawPassword){

        // loginId로 회원을 조회한 뒤 비밀번호가 일치하면 해당 회원 반환
        // Optional의 filter 사용
        return memberRepository.findByLoginId(loginId)
                .filter(m -> passwordEncoder.matches(rawPassword, m.getPassword()))
                .orElse(null);
    }

    // 회원 정보 수정
    @Transactional
    public void editMember(Long memberId, Member memberParam){

        // 수정할 회원 조회
        Member findMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        // 수정할 정보 중복 검사
        validateDuplicateMemberForEdit(findMember ,memberParam);

        // 정보 수정 (도메인 내부에 위치)
        findMember.changeMemberInfo(memberParam.getLoginId(), passwordEncoder.encode(memberParam.getPassword()),
                memberParam.getEmail(), memberParam.getNickname());
    }

    // 회원 탈퇴
    @Transactional
    public void withdrawMember(Long memberId) {
        Member findMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        findMember.withdraw();
    }

    // loginId, email, nickname 중복 여부 검증 (existsBy 적용)
    private void validateDuplicateMember(Member member) {
        if(memberRepository.existsByLoginId(member.getLoginId())){
            throw new IllegalStateException(("동일한 ID가 존재합니다."));
        }

        if(memberRepository.existsByEmail(member.getEmail())){
            throw new IllegalStateException("동일한 EMAIL이 존재합니다.");
        }

        if(memberRepository.existsByNickname(member.getNickname())){
            throw new IllegalStateException("동일한 NICKNAME이 존재합니다.");
        }
    }

    // 정보 수정용 중복 검사
    // 기존 회원의 정보와 폼에서 넘어온 새로운 정보가 다를 경우에만 중복 검사를 실행하도록 방어 로직 추가
    private void validateDuplicateMemberForEdit(Member findMember, Member memberParam){
        if(!findMember.getLoginId().equals(memberParam.getLoginId())
            && memberRepository.existsByLoginId(memberParam.getLoginId())){
            throw new IllegalStateException(("동일한 ID가 존재합니다."));
        }

        if(!findMember.getEmail().equals(memberParam.getEmail())
                && memberRepository.existsByEmail(memberParam.getEmail())){
            throw new IllegalStateException("동일한 EMAIL이 존재합니다.");
        }

        if(!findMember.getNickname().equals(memberParam.getNickname())
                && memberRepository.existsByNickname(memberParam.getNickname())){
            throw new IllegalStateException("동일한 NICKNAME이 존재합니다.");
        }
    }
}