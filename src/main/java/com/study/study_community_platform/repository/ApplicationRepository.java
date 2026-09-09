package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Application;
import com.study.study_community_platform.domain.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByStudyId(Long studyId);
    List<Application> findByMemberIdAndStudyId(Long memberId, Long studyId);

    // 삭제된 스터디의 신청은 목록에서 제외
    // 화면에 필요한 Study를 함께 조회해 지연 로딩 중 조회 예외를 방지
    @Query("""
            select a
            from Application a
            join fetch a.study s
            where a.member.id = :memberId
            and s.deletedAt is null
            order by a.createdAt desc
            """
            )
    List<Application> findActiveStudyApplicationsByMemberId(
            @Param("memberId") Long memberId
    );

    long countByStudyIdAndStatus(Long studyId, ApplicationStatus status);
}