package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Study;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class StudyRepository {

    private final EntityManager em;

    // 신규 스터디는 persist, 기존 스터디는 merge
    public void save(Study study) {
        if (study.getId() == null) {
            em.persist(study);
        } else {
            em.merge(study);
        }
    }

    // PK 기준 단건 조회
    public Optional<Study> findById(Long id){
        return Optional.ofNullable(em.find(Study.class, id));
    }

    // 전체 스터디 조회
    public List<Study> findAll(){
        return em.createQuery("select s from Study s", Study.class)
                .getResultList();
    }

    // 스터디 삭제
    public void delete(Long id){
        Study study = em.find(Study.class, id);
        if (study != null) {
            em.remove(study);
        }
    }
}