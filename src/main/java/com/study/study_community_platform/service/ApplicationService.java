package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.*;
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

        if(isApplied(memberId, studyId)){
            throw new IllegalStateException("신청 대기 중이거나 이미 승인된 스터디입니다.");
        }

        // 스터디 신청 엔티티 생성
        Application application = Application.createApplication(member, study, message);

        // 신청 저장
        applicationRepository.save(application);

        return application.getId();
    }

    // 동일 회원이 동일 스터디에 중복 신청했는지 확인
    public boolean isApplied(Long memberId, Long studyId) {
        // 취소(CANCELED)나 거절(REJECTED)된 건 무시하고
        // PENDING(보류) 이나 APPROVED(승인) 상태인 신청건이 있는지 확인하는 것으로 수정
        return applicationRepository.findByMemberIdAndStudyId(memberId, studyId).stream()
                .anyMatch(app -> app.getStatus() == ApplicationStatus.PENDING || app.getStatus() == ApplicationStatus.APPROVED);
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

    // 현재 로그인한 회원이 신청 대상 스터디의 작성자인지 검증
    // 브라우저가 전달한 studyId가 아닌 Application에 연결된 실제 Study의 작성자 ID를 사용하는 것으로 수정
    private void validateStudyOwner(Long loginMemberId, Application application){
        Long studyOwnerId = application.getStudy().getMember().getId();

        if(!studyOwnerId.equals(loginMemberId)){
            throw new IllegalStateException("스터디 작성자만 신청을 승인하거나 거절할 수 있습니다.");
        }
    }

    // 현재 로그인한 회원이 실제 신청자인지 검증
    private void validateApplicant(Long loginMemberId, Application application){
        Long applicantId = application.getMember().getId();

        if (!applicantId.equals(loginMemberId)) {
            throw new IllegalStateException("신청자 본인만 신청을 취소할 수 있습니다.");
        }
    }

    // 스터디 신청 승인
    @Transactional
    public void approveApplication(Long loginMemberId, Long applicationId){

        // 신청 엔티티 및 해당하는 스터디 엔티티 조회
        Application application = findApplication(applicationId);

        // 정원 검사나 상태 변경 전에 실제 신청 대상 스터디의 작성자인지 검증
        validateStudyOwner(loginMemberId, application);

        Study study = application.getStudy();

        // 현재 승인된 인원수를 조회해서 정원 초과 방어
        long approvedCount = applicationRepository.countByStudyIdAndStatus(study.getId(), ApplicationStatus.APPROVED);
        if(approvedCount >= study.getCapacity()){
            throw new IllegalStateException("스터디 모집 정원이 꽉 차서 더 이상 승인할 수 없습니다.");
        }

        // 엔티티 내부 상태 변경
        application.approve();

        // 승인 후 정원이 다 찼다면 스터디 자동 마감
        if(approvedCount + 1 >= study.getCapacity()){
            study.close();
        }
    }

    // 스터디 신청 거절
    @Transactional
    public void rejectApplication(Long loginMemberId, Long applicationId){

        Application application = findApplication(applicationId);

        // Application과 연결된 실제 Study의 작성자만 해당 신청 거절 가능
        validateStudyOwner(loginMemberId, application);

        application.reject();
    }

    // 스터디 신청 취소
    @Transactional
    public void cancelApplication(Long loginMemberId, Long applicationId) {

        Application application = findApplication(applicationId);

        // 신청을 작성한 회원 본인인지 확인한 후 취소
        validateApplicant(loginMemberId, application);

        application.cancel();
    }

}