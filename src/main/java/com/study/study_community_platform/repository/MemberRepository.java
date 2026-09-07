package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 단순 JPA -> 스프링 데이터 JPA 적용
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 기존 단건 조회는 탈퇴한 회원도 조회 가능 -> 탈퇴하지 않은 회원만 로그인 ID로 조회할수 있게 수정
    Optional<Member> findByLoginIdAndDeletedAtIsNull(String loginId);

    // 탈퇴하지 않은 회원만 PK로 조회
    Optional<Member> findByIdAndDeletedAtIsNull(Long memberId);

    // 조건에 맞는 데이터가 있는지 boolean 값만 반환받도록 처리(EXISTS 쿼리 발생) -> 쿼리 성능 최적화
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}