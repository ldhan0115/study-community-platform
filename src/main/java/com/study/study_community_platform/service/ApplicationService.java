package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Application;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyStatus;
import com.study.study_community_platform.repository.ApplicationRepository;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final MemberRepository memberRepository;
    private final StudyRepository studyRepository;

    @Transactional
    public Long applyToStudy(Long memberId, Long studyId, String message){

        // 신청을 하는 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 신청 대상 스터디 존재 여부 확인
        Study study = studyRepository.findById(studyId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디입니다."));

        // 스터디 모집 상태 확인
        // CLOSED 상태라면 더 이상 신청 불가
        if(study.getStudyStatus() == StudyStatus.CLOSED){
            throw new IllegalStateException("모집이 마감된 스터디입니다.");
        }

        // 스터디 정원 초과 여부 확인
        // 현재 신청 수가 정원(capacity) 이상이면 신청 불가
        if(study.getCapacity() <= applicationRepository.findByStudyId(studyId).size()){
            throw new IllegalStateException("스터디 정원이 가득 찼습니다.");
        }

        // 스터디 신청 엔티티 생성
        Application application = Application.createApplication(member, study, message);

        // 신청 저장
        applicationRepository.save(application);

        return application.getId();
    }

    // 동일 회원이 동일 스터디에 중복 신청했는지 확인
    public boolean isApplied(Long memberId, Long studyId) {
        List<Application> existingApplication  = applicationRepository.findByMemberIdAndStudyId(memberId, studyId);
        if(!existingApplication.isEmpty()){
            return true;
        }
        return false;
    }

    // 신청 단건 조회
    public Application findApplication(Long applicationId){
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));
    }

    // 특정 스터디의 신청 목록 조회
    public List<Application> findApplicationsByStudy(Long studyId){
        return applicationRepository.findByStudyId(studyId);
    }

    // 특정 회원의 신청 목록 조회
    public List<Application> findApplicationsByMember(Long memberId){
        return applicationRepository.findByMemberId(memberId);
    }

    // 스터디 신청 승인
    @Transactional
    public void approveApplication(Long applicationId){

        // 신청 엔티티 조회
        Application application = getApplication(applicationId);

        // 엔티티 내부 상태 변경
        application.approve();
    }

    // 스터디 신청 거절
    @Transactional
    public void rejectApplication(Long applicationId){

        Application application = getApplication(applicationId);

        application.reject();
    }

    // 스터디 신청 취소
    @Transactional
    public void cancelApplication(Long applicationId) {

        Application application = getApplication(applicationId);

        application.cancel();
    }

    // 공통 신청 조회 메서드
    // 서비스 내부에서 반복되는 조회 로직을 분리
    private Application getApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));
    }
}