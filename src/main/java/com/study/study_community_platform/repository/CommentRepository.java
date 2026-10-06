package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    List<Comment> findByMemberId(Long memberId);
    List<Comment> findByStudyId(Long studyId);

    @Query("""
            select c from Comment c
            join fetch c.study s
            where c.id = :commentId
            and c.deletedAt is null
            and s.deletedAt is null
            """)
    Optional<Comment> findActiveById(@Param("commentId") Long commentId);

}
