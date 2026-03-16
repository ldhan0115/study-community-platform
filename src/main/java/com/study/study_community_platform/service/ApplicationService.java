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

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Study study = studyRepository.findById(studyId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 스터디입니다."));

        if(study.getStudyStatus() == StudyStatus.CLOSED){
            throw new IllegalStateException("모집이 마감된 스터디입니다.");
        }

        if(study.getCapacity() <= applicationRepository.findByStudyId(studyId).size()){
            throw new IllegalStateException("스터디 정원이 가득 찼습니다.");
        }

        List<Application> existingApplication  = applicationRepository.findByMemberIdAndStudyId(memberId, studyId);
        if(!existingApplication.isEmpty()){
            throw new IllegalStateException("이미 신청한 스터디입니다.");
        }

        Application application = Application.createApplication(member, study, message);

        applicationRepository.save(application);
        return application.getId();
    }

    public Application findApplication(Long applicationId){
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));
    }

    public List<Application> findApplicationsByStudy(Long studyId){
        return applicationRepository.findByStudyId(studyId);
    }

    public List<Application> findApplicationsByMember(Long memberId){
        return applicationRepository.findByMemberId(memberId);
    }

    @Transactional
    public void approveApplication(Long applicationId){
        Application application = getApplication(applicationId);
        application.approve();
    }

    @Transactional
    public void rejectApplication(Long applicationId){
        Application application = getApplication(applicationId);
        application.reject();
    }

    @Transactional
    public void cancelApplication(Long applicationId) {
        Application application = getApplication(applicationId);
        application.cancel();

    }

    private Application getApplication(Long applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 신청입니다."));
    }
}
