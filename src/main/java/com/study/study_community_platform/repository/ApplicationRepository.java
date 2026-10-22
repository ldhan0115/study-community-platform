package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Application;
import com.study.study_community_platform.domain.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // 신청자 관리 화면에서 사용하는 신청자 정보를 함께 조회
    @Query("""
            select a
            from Application a
            join fetch a.member
            where a.study.id = :studyId
            order by a.createdAt desc, a.id desc
            """)
    List<Application> findByStudyId(@Param("studyId") Long studyId);
    List<Application> findByMemberIdAndStudyId(Long memberId, Long studyId);

    // 삭제된 스터디의 신청은 목록에서 제외
    // 화면에 필요한 Study를 함께 조회해 지연 로딩 중 조회 예외를 방지
    // 내 신청 내역은 스터디 제목뿐 아니라 스터디 작성자의 닉네임도 사용하므로 같이 조회
    @Query("""
            select a
            from Application a
            join fetch a.study s
            join fetch s.member
            where a.member.id = :memberId
             and s.deletedAt is null
            order by a.createdAt desc, a.id desc
            """
            )
    List<Application> findActiveStudyApplicationsByMemberId(
            @Param("memberId") Long memberId
    );

    long countByStudyIdAndStatus(Long studyId, ApplicationStatus status);

    // 부모 스터디가 활성 상태인 신청만 조회
    // Study를 함께 조회해서 이후 부모 접근을 지연 로딩 예외에 맡기지 않음
    @Query("""
            select a from Application a
            join fetch a.study s 
            where a.id = :applicationId
            and s.deletedAt is null
            """)
    Optional<Application> findActiveById(@Param("applicationId") Long applicationId);

    @Query("""
            select a.study.id
            from Application a
            where a.id = :applicationId
            """)
    Optional<Long> findStudyIdByApplicationId(@Param("applicationId") Long applicationId);

    // 신청 이력을 엔티티 목록으로 가져오지 않고 존재 여부만 확인
    boolean existsByMemberIdAndStudyIdAndStatusIn(Long memberId,
                                                  Long studyId,
                                                  Collection<ApplicationStatus> statuses);
}