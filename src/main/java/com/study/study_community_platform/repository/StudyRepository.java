package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Study;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyRepository extends JpaRepository<Study, Long> {

    Optional<Study> findByIdAndDeletedAtIsNull(Long studyId);
}