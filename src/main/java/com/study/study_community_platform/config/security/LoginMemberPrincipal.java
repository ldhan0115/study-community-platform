package com.study.study_community_platform.config.security;

import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.Member;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.io.Serial;
import java.util.List;

// Security 전용 회원 객체
// Spring Security의 User를 상속하므로 UserDetails의 역할
public class LoginMemberPrincipal extends User {

    @Serial
    private static final long serialVersionUID = 1L;

    // 회원을 식별하기 위한 PK
    private final Long memberId;
    // 화면에 표시할 닉네임
    private final String nickname;

    private LoginMemberPrincipal(Member member){

        /*
        부모인 Spring Security User에 전달하는 정보
        username    : 로그인 아이디
        password    : DB에 저장된 암호화 비밀번호
        authorities : 현재 사용자의 권한
         */
        super(
                member.getLoginId(),
                member.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        this.memberId = member.getId();
        this.nickname = member.getNickname();
    }

    public static LoginMemberPrincipal from(Member member){
        return new LoginMemberPrincipal(member);
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getNickname() {
        return nickname;
    }

    // Controller와 화면에서 사용할 최소 세션 DTO로 변환
    public LoginMemberSession toLoginMemberSession(){
        return new LoginMemberSession(memberId, nickname);
    }
}
