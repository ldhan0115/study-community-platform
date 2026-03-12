package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Member;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final EntityManager em;

    // 신규 회원은 persist, 기존 회원은 merge로 저장
    public void save(Member member) {
        if (member.getId() == null) {
            em.persist(member);
        } else {
            em.merge(member);
        }
    }

    // PK 기준 회원 단건 조회
    // 조회 결과가 없을 수 있다는 걸 감안해서 Optional로 감싸서 반환
    public Optional<Member> findById(Long id) {
        return Optional.ofNullable(em.find(Member.class, id));
    }

    // 전체 회원 조회
    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }
}