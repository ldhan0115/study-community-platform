package com.study.study_community_platform.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
// JPA에서 사용하는 기본 생성자, 외부에서 직접 생성하는 것을 방지
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    // 회원의 기본 키 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // DB auto increment 방식 사용
    @Column(name = "member_id")
    private Long id;

    // 로그인에 사용하는 아이디 (중복 불가)
    @Column(nullable = false, unique = true, length = 50)
    private String loginId;

    // 비밀번호 (추후 암호화된 값으로 저장)
    @Column(nullable = false, length = 255)
    private String password;

    // 이메일 (회원 식별 및 알림 등에 사용)
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 커뮤니티에서 표시되는 닉네임
    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    // 회원 생성 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 회원 정보 수정 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 회원 정보 삭제 시간
    // soft delete용 컬럼 (값이 있으면 삭제된 회원)
    private LocalDateTime deletedAt;

    // 회원 객체 생성 시 사용하는 생성자
     public static Member createMember(String loginId, String password, String email, String nickname) {
         Member member = new Member();
         member.loginId = loginId;
         member.password = password;
         member.email = email;
         member.nickname = nickname;
         return member;
    }

    // 회원 정보 수정 메서드
    public void changeMemberInfo(String loginId, String password, String email, String nickname){
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.nickname = nickname;
    }

    // 회원 객체가 저장되기 직전에 생성/수정 시간을 세팅
    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 회원 객체 수정 직전에 수정 시간을 갱신
    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    // 회원 탈퇴 시 현재 시간으로 갱신 -> 소프트 딜리트(Soft Delete) 방식
    public void withdraw(){
         this.deletedAt = LocalDateTime.now();
    }
}