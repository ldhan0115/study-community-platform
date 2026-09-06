package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.*;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@Profile("test")
class ApplicationServiceTest {

    @Autowired ApplicationService applicationService;
    @Autowired MemberRepository memberRepository;
    @Autowired StudyRepository studyRepository;

    @Test
    void applyToStudy(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Study study = Study.createStudy(member, "JPA", "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 10);
        studyRepository.save(study);

        //when
        Long applicationId = applicationService.applyToStudy(member.getId(), study.getId(), "열심히 하겠습니다.");

        //then
        Application findApplication = applicationService.findApplication(applicationId);
        assertThat(findApplication.getMember()).isEqualTo(member);
        assertThat(findApplication.getStudy()).isEqualTo(study);
        assertThat(findApplication.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void otherStudyOwnerCannotApproveApplication(){
        // given
        Member otherStudyOwner = Member.createMember("otherOwner", "1234",
                "otherOwner@test.com", "아더오너");

        Member targetStudyOwner = Member.createMember("targetOwner",
                "1234", "targetOwner@test.com", "실제오너");

        Member applicant = Member.createMember("applicant", "1234",
                "applicant@test.com", "신청자"
        );

        memberRepository.saveAll(List.of(otherStudyOwner, targetStudyOwner, applicant));

        Study otherStudy = Study.createStudy(otherStudyOwner, "다른 스터디",
                "다른 내용", StudyMethod.ONLINE, null, 5);

        Study targetStudy = Study.createStudy(targetStudyOwner, "실제 스터디",
                "실제 내용", StudyMethod.ONLINE, null, 5
        );

        studyRepository.saveAll(List.of(otherStudy, targetStudy));

        Long applicationId = applicationService.applyToStudy(applicant.getId(),
                targetStudy.getId(), "참여하고 싶습니다.");

        Application application = applicationService.findApplication(applicationId);

        //when & then
        assertThatThrownBy(() ->
                applicationService.approveApplication(otherStudyOwner.getId(), applicationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("스터디 작성자만 신청을 승인하거나 거절할 수 있습니다.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void otherMemberCannotRejectApplication(){
        // given
        Member studyOwner = Member.createMember("studyOwner",
                "1234", "studyOwner@test.com", "오너");

        Member applicant = Member.createMember("applicant", "1234",
                "applicant@test.com", "신청자");

        Member other = Member.createMember("other", "1234",
                "other@test.com", "아더");


        memberRepository.saveAll(List.of(studyOwner, applicant, other));

        Study study = Study.createStudy(studyOwner, "스터디",
                "내용", StudyMethod.ONLINE, null, 5
        );

        studyRepository.save(study);

        Long applicationId = applicationService.applyToStudy(applicant.getId(), study.getId(), "참여하고 싶습니다.");

        Application application = applicationService.findApplication(applicationId);

        //when & then
        assertThatThrownBy(() ->
                applicationService.rejectApplication(other.getId(), applicationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("스터디 작성자만 신청을 승인하거나 거절할 수 있습니다.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void otherMemberCannotCancelApplication(){
        // given
        Member studyOwner = Member.createMember("studyOwner",
                "1234", "studyOwner@test.com", "오너");

        Member applicant = Member.createMember("applicant", "1234",
                "applicant@test.com", "신청자");

        Member other = Member.createMember("other", "1234",
                "other@test.com", "아더");


        memberRepository.saveAll(List.of(studyOwner, applicant, other));

        Study study = Study.createStudy(studyOwner, "스터디",
                "내용", StudyMethod.ONLINE, null, 5
        );

        studyRepository.save(study);

        Long applicationId = applicationService.applyToStudy(applicant.getId(), study.getId(), "참여하고 싶습니다.");

        Application application = applicationService.findApplication(applicationId);

        //when & then
        assertThatThrownBy(() ->
                applicationService.cancelApplication(other.getId(), applicationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("신청자 본인만 신청을 취소할 수 있습니다.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void applicantCanCancelOwnApplication(){
        // given
        Member studyOwner = Member.createMember("studyOwner",
                "1234", "studyOwner@test.com", "오너");

        Member applicant = Member.createMember("applicant", "1234",
                "applicant@test.com", "신청자");

        memberRepository.saveAll(List.of(studyOwner, applicant));

        Study study = Study.createStudy(studyOwner, "스터디",
                "내용", StudyMethod.ONLINE, null, 5
        );

        studyRepository.save(study);

        Long applicationId = applicationService.applyToStudy(applicant.getId(), study.getId(), "참여하고 싶습니다.");

        Application application = applicationService.findApplication(applicationId);

        //when
        applicationService.cancelApplication(applicant.getId(), applicationId);

        // then
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.CANCELED);
    }

    @Test
    void closeStudyApplication(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "t    ester");
        memberRepository.save(member);

        Study study = Study.createStudy(member, "JPA", "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 10);
        studyRepository.save(study);

        study.close();

        //when
        assertThatThrownBy(() -> applicationService.applyToStudy(member.getId(), study.getId(), "열심히 하겠습니다."))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("모집이 마감된 스터디입니다.");
    }

    @Test
    void fullCapacity(){
        // given
        Member member1 = Member.createMember("test1", "1234", "test1@gmail.com", "tester1");
        Member member2 = Member.createMember("test2", "1234", "test2@gmail.com", "tester2");
        Member member3 = Member.createMember("test3", "1234", "test3@gmail.com", "tester3");
        memberRepository.save(member1);
        memberRepository.save(member2);
        memberRepository.save(member3);

        Study study = Study.createStudy(member1, "JPA", "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 1);
        studyRepository.save(study);

        Long application1 = applicationService.applyToStudy(member2.getId(), study.getId(), "열심히 하겠습니다.");
        Long application2 = applicationService.applyToStudy(member3.getId(), study.getId(), "열심히 하겠습니당.");

        applicationService.approveApplication(member1.getId(), application1);

        //when
        assertThatThrownBy(() -> applicationService.approveApplication(member1.getId(), application2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("스터디 모집 정원이 꽉 차서 더 이상 승인할 수 없습니다.");

    }

    @Test
    void sameApplication(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Study study = Study.createStudy(member, "JPA", "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 10);
        studyRepository.save(study);

        applicationService.applyToStudy(member.getId(), study.getId(), "열심히 하겠습니다.");

        //when
        assertThatThrownBy(() -> applicationService.applyToStudy(member.getId(), study.getId(), "또 왔어요"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("신청 대기 중이거나 이미 승인된 스터디입니다.");
    }

    @Test
    void findApplicationByStudy(){
        // given
        Member member1 = Member.createMember("test1", "1234", "test1@gmail.com", "tester1");
        Member member2 = Member.createMember("test2", "1234", "test2@gmail.com", "tester2");
        memberRepository.save(member1);
        memberRepository.save(member2);

        Study study = Study.createStudy(member1, "JPA", "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 10);
        studyRepository.save(study);

        Long applicationId1 = applicationService.applyToStudy(member1.getId(), study.getId(), "member1");
        Long applicationId2 = applicationService.applyToStudy(member2.getId(), study.getId(), "member2");

        Application application1 = applicationService.findApplication(applicationId1);
        Application application2 = applicationService.findApplication(applicationId2);

        //when
        List<Application> applicationsByStudy = applicationService.findApplicationsByStudy(study.getId());

        //then
        assertThat(applicationsByStudy.size()).isEqualTo(2);
        assertThat(applicationsByStudy).contains(application1, application2);
    }

    @Test
    void findApplicationsByMember(){
        // given
        Member member = Member.createMember("test1", "1234", "test1@gmail.com", "tester1");
        memberRepository.save(member);

        Study study1 = Study.createStudy(member, "JPA", "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 10);
        studyRepository.save(study1);

        Study study2 = Study.createStudy(member, "JPA", "SPRING을 열심히 공부해요", StudyMethod.OFFLINE, "서울", 10);
        studyRepository.save(study2);


        Long applicationId1 = applicationService.applyToStudy(member.getId(), study1.getId(), "화이팅");
        Long applicationId2 = applicationService.applyToStudy(member.getId(), study2.getId(), "화이팅");

        //when
        List<Application> applicationsByMember = applicationService.findApplicationsByMember(member.getId());

        //then
        assertThat(applicationsByMember.size()).isEqualTo(2);
        assertThat(applicationsByMember)
                .extracting(application -> application.getMember().getId())
                .containsOnly(member.getId());
    }

    @Test
    void approvedApplicationCannotBeRejected(){
        // given
        Member studyOwner = Member.createMember("studyOwner",
                "1234", "studyOwner@test.com", "오너");

        Member applicant = Member.createMember("applicant", "1234",
                "applicant@test.com", "신청자");

        memberRepository.saveAll(List.of(studyOwner, applicant));

        Study study = Study.createStudy(studyOwner, "스터디",
                "내용", StudyMethod.ONLINE, null, 5
        );

        studyRepository.save(study);

        Long applicationId = applicationService.applyToStudy(applicant.getId(),
                study.getId(), "참여하고 싶습니다.");

        Application application = applicationService.findApplication(applicationId);

        applicationService.approveApplication(studyOwner.getId(), applicationId);

        //when & then
        assertThatThrownBy(() ->
                applicationService.rejectApplication(studyOwner.getId(), applicationId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("대기 중인 신청만 상태를 변경할 수 있습니다.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    }

}