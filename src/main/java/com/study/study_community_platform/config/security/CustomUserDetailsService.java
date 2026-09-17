package com.study.study_community_platform.config.security;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
// DB에서 회원과 암호화된 비밀번호 조회하는 클래스
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    // Spring Security는 프로젝트의 MemberRepository 구조를 알지 못함
    // -> 따라서 로그인 ID로 회원을 어떻게 찾는지 알려줌
    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        Member member = memberRepository.findByLoginIdAndDeletedAtIsNull(loginId)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않거나 탈퇴한 회원입니다."));

        // Member 엔티티를 Spring Security가 이해할 수 있는 객체로 변환
        return LoginMemberPrincipal.from(member);
    }
}
