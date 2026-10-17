package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Study;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long> {

    Optional<Study> findByIdAndDeletedAtIsNull(Long studyId);

    // 같은 스터디를 변경하는 요청들이 공통으로 사용하는 행 잠금
    // 잠금은 이 메서드가 끝날 때가 아니라 서비스 트랜잭션 종료 시 해제
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from Study s
            where s.id = :studyId
            and s.deletedAt is null
            """)
    Optional<Study> findActiveByIdForUpdate(@Param("studyId") Long studyId);
}