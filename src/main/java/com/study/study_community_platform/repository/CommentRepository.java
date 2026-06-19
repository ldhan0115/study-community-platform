package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Comment;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CommentRepository {

    private final EntityManager em;

    // 신규 댓글은 persist, 기존 댓글은 merge
    public void save(Comment comment){

        if (comment.getId() == null) {
            em.persist(comment);

        } else {
            em.merge(comment);
        }
    }

    // PK 기준 단건 조회
    public Optional<Comment> findById(Long id){
        return Optional.ofNullable(em.find(Comment.class, id));
    }

    // 회원 기준 댓글 목록 조회
    public List<Comment> findByMemberId(Long memberId){

        return em.createQuery("select c from Comment c where c.member.id = :memberId", Comment.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    // 게시물 기준 댓글 목록 조회
    public List<Comment> findByStudyId(Long studyId){

        return em.createQuery("select c from Comment c where c.study.id = :studyId", Comment.class)
                .setParameter("studyId", studyId)
                .getResultList();
    }

    // 전체 댓글 조회
    public List<Comment> findAll(){
        return em.createQuery("select c from Comment c", Comment.class)
                .getResultList();
    }

    // 댓글 삭제
    public void delete(Long id){
        Comment comment = em.find(Comment.class, id);

        if (comment != null) {
            em.remove(comment);
        }
    }

}
