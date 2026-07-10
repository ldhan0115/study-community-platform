package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Application;
import com.study.study_community_platform.domain.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByStudyId(Long studyId);
    List<Application> findByMemberId(Long memberId);
    List<Application> findByMemberIdAndStudyId(Long memberId, Long studyId);

    long countByStudyIdAndStatus(Long studyId, ApplicationStatus status);
}