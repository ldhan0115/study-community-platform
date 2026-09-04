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

    // 스터디 등록
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

    // 전체 스터디 조회
    public List<Study> findStudies(){
        return studyRepository.findAll();
    }

    // 스터디 단건 조회
    public Study findStudy(Long studyId){
        return studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디입니다."));

    }

    // 스터디 작성자만 수정 화면에 접근할 수 있도록 조회와 권한 검증을 함께 수행
    public Study findStudyForOwner(Long loginMemberId, Long studyId){
        Study study = findStudy(studyId);
        validateStudyOwner(loginMemberId, study);
        return study;
    }

    // 현재 요청한 회원이 스터디 작성자인지 검증
    // 다른 코드가 Service를 직접 호출했을 때 검증을 우회할 수 있으므로 Service 내부에서 검사
    private void validateStudyOwner(Long loginMemberId, Study study){
        if(!study.getMember().getId().equals(loginMemberId)){
            throw new IllegalStateException("스터디 작성자만 수정하거나 삭제할 수 있습니다.");
        }
    }

    // 스터디 수정
    @Transactional
    public void updateStudy(Long loginMemberId, Long studyId,  String title, String content,
                            StudyMethod method, String region, int capacity) {

        Study findStudy = findStudy(studyId);

        // 엔티티를 변경하기 전에 작성자를 검증
        validateStudyOwner(loginMemberId, findStudy);

        // 조회한 기존 엔티티의 값만 변경해서 JPA dirty checking으로 반영
        findStudy.changeStudyInfo(title, content, method, region, capacity);
    }

    // 스터디 삭제
    @Transactional
    public void deleteStudy(Long loginMemberId, Long studyId){
        Study study = findStudy(studyId);

        // 삭제 처리 전에 작성자를 검증
        validateStudyOwner(loginMemberId, study);

        study.withdraw();
    }

    // 스터디 모집 마감 처리
    @Transactional
    public void closeStudy(Long studyId){
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디입니다."));

        // 스터디 객체 내부에 비즈니스 로직 위치
        study.close();
    }
}