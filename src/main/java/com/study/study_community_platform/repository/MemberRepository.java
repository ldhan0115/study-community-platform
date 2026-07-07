package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 단순 JPA -> 스프링 데이터 JPA 적용
public interface MemberRepository extends JpaRepository<Member, Long> {

    // 단건 조회
    Optional<Member> findByLoginId(String loginId);

    // 조건에 맞는 데이터가 있는지 boolean 값만 반환받도록 처리(EXISTS 쿼리 발생) -> 쿼리 성능 최적화
    boolean existsByLoginId(String loginId);
    boolean existsByEmail(String email);
    boolean existsByNickname(String nickname);
}