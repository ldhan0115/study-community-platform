package com.study.study_community_platform.service;

import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.service.dto.MemberUpdateDto;
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
    public Member editMember(Long memberId, MemberUpdateDto updateDto){

        // 수정할 회원 조회
        Member findMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        // 수정할 정보 중복 검사
        validateDuplicateMemberForEdit(findMember ,updateDto);

        // 정보 수정 (도메인 내부에 위치)
        // 비밀번호는 평문 그대로 저장하지 않고 PasswordEncoder로 암호화한 후 저장
        findMember.changeMemberInfo(
                updateDto.loginId(),
                passwordEncoder.encode(updateDto.password()),
                updateDto.email(),
                updateDto.nickname()
        );

        // Controller에서 수정된 영속 엔티티로 세션을 갱신하도록 반환
        return findMember;
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
    private void validateDuplicateMemberForEdit(Member findMember, MemberUpdateDto updateDto){
        if(!findMember.getLoginId().equals(updateDto.loginId())
            && memberRepository.existsByLoginId(updateDto.loginId())){
            throw new IllegalStateException(("동일한 ID가 존재합니다."));
        }

        if(!findMember.getEmail().equals(updateDto.email())
                && memberRepository.existsByEmail(updateDto.email())){
            throw new IllegalStateException("동일한 EMAIL이 존재합니다.");
        }

        if(!findMember.getNickname().equals(updateDto.nickname())
                && memberRepository.existsByNickname(updateDto.nickname())){
            throw new IllegalStateException("동일한 NICKNAME이 존재합니다.");
        }
    }
}