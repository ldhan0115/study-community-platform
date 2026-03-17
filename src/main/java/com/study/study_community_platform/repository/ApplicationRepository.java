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

    // 회원 기준 신청 목록 조회
    public List<Application> findByMemberId(Long memberId){

        return em.createQuery("select a from Application a where a.member.id = :memberId", Application.class)
                .setParameter("memberId", memberId)
                .getResultList();
    }

    // 스터디 기준 신청 목록 조회
    public List<Application> findByStudyId(Long studyId){

        return em.createQuery("select a from Application a where a.study.id = :studyId", Application.class)
                .setParameter("studyId", studyId)
                .getResultList();
    }

    // memberId와 studyId 쌍으로 해당하는 신청 조회 -> 중복 신청 방지에 사용
    public List<Application> findByMemberIdAndStudyId(Long memberId, Long studyId){

        return em.createQuery(
                "select a from Application a " +
                        "where a.member.id = :memberId and a.study.id = :studyId", Application.class
                )
                .setParameter("memberId", memberId)
                .setParameter("studyId", studyId)
                .getResultList();
    }

    // 전체 신청 조회
    public List<Application> findAll(){

        return em.createQuery("select a from Application a", Application.class)
                .getResultList();
    }

}