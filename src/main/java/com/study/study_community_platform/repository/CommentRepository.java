package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByMemberId(Long memberId);
    List<Comment> findByStudyId(Long studyId);

}
