package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Study;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudyRepository extends JpaRepository<Study, Long> {

}