package com.study.study_community_platform.service.dto;

// 회원 수정에 필요한 값을 Service에 전달하는 DTO
//  단순한 수정 데이터를 전달하기 위해 새로운 Member 엔티티를 생성하지 않고 별도의 요청 객체를 사용
public record MemberUpdateDto(
        String loginId,
        String password,
        String email,
        String nickname
){
}

