package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Post;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class PostRepository {

    private final EntityManager em;

    public void save(Post post){

        // 신규 게시물은 persist, 기존 게시물은 merge
        if (post.getId() == null) {
            em.persist(post);

        } else {
            em.merge(post);
        }
    }

    // PK 기준 단건 조회
    public Optional<Post> findById(Long id){

        return Optional.ofNullable(em.find(Post.class, id));
    }

    // 회원 기준 게시물 목록 조회
    public List<Post> findByMemberId(Long memberId){

        return em.createQuery("select p from Post p where p.member.id = :memberId", Post.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    // 전체 게시물 조회
    public List<Post> findAll(){
        return em.createQuery("select p from Post p", Post.class)
                .getResultList();
    }

    // 게시물 삭제
    public void delete(Long id){
        Post post = em.find(Post.class, id);

        if (post != null) {
            em.remove(post);
        }
    }
}