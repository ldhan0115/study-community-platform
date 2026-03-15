package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudyService {

    private final StudyRepository studyRepository;
    private final MemberRepository memberRepository;

    // 스터디 생성
    @Transactional
    public Long registerStudy(Long memberId, String title, String content,
                              StudyMethod method, String region, int capacity){

        // 스터디 작성자 조회
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // Study 객체 생성 메서드를 사용해 생성 규칙을 한 곳에서 관리
        Study study = Study.createStudy(member, title, content, method, region, capacity);

        studyRepository.save(study);
        return study.getId();
    }

    // 스터디 단건 조회
    public Study findStudy(Long studyId){
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디입니다."));
    }

    // 전체 스터디 조회
    public List<Study> findStudies(){
        return studyRepository.findAll();
    }

    // 스터디 수정
    @Transactional
    public void updateStudy(Long studyId,  String title, String content,
                            StudyMethod method, String region, int capacity) {

        Study findStudy = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디입니다."));

        // 조회한 기존 엔티티의 값만 변경해서 JPA dirty checking으로 반영
        findStudy.changeStudyInfo(title, content, method, region, capacity);
    }


    // 스터디 모집 마감 처리
    @Transactional
    public void closeStudy(Long studyId){
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디입니다."));

        // 스터디 객체 내부에 비즈니스 로직 위치
        study.close();
    }

    // 스터디 삭제
    @Transactional
    public void deleteStudy(Long studyId){
        studyRepository.delete(studyId);
    }
}