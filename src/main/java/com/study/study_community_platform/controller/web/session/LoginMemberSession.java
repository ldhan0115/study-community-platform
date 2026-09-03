package com.study.study_community_platform.controller.web.session;

import com.study.study_community_platform.domain.Member;

import java.io.Serializable;

// 로그인 세션에 저장할 최소 회원 정보
// 기존 Member 엔티티 전체를 저장한 것에서 필요한 부분만 저장하는 것으로 변경
public record LoginMemberSession(
        Long id,
        String nickname
) implements Serializable{ // Serializable은 이후 Redis나 세션 클러스터링처럼 세션을 외부에 저장할 가능성에 대비

    // Member 엔티티에서 세션에 필요한 정보만 추출
    public static LoginMemberSession from(Member member){
        return new LoginMemberSession(
                member.getId(),
                member.getNickname()
        );
    }
}
