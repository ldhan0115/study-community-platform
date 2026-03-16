package com.study.study_community_platform.repository;

import com.study.study_community_platform.domain.Application;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ApplicationRepository {

    private final EntityManager em;

    // 신규 신청 저장
    public void save(Application application){
        em.persist(application);
    }

    // PK 기준 신청 단건 조회
    public Optional<Application> findById(Long id) {
        return Optional.ofNullable(em.find(Application.class, id));
    }

    // 스터디 기준 신청 목록 조회
    public List<Application> findByStudyId(Long studyId){

        return em.createQuery("select a from Application a where a.study.id = :studyId", Application.class)
                .setParameter("studyId", studyId)
                .getResultList();
    }

    // 전체 신청 조회
    public List<Application> findAll(){

        return em.createQuery("select a from Application a", Application.class)
                .getResultList();
    }

}