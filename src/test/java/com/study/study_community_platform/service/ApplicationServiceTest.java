package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.*;
import com.study.study_community_platform.exception.BusinessRuleException;
import com.study.study_community_platform.exception.ForbiddenOperationException;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ApplicationServiceTest {

    @Autowired
    ApplicationService applicationService;
    @Autowired
    MemberRepository memberRepository;
    @Autowired
    StudyRepository studyRepository;
    @Autowired
    StudyService studyService;
    @Autowired
    EntityManager em;

    @Test
    void applyToStudy() {

        // given
        Member owner = saveTestMember("owner");
        Member applicant = saveTestMember("applicant");
        Study study = saveTestStudy(owner, 5);

        // when
        Long applicationId = applicationService.applyToStudy(
                applicant.getId(),
                study.getId(),
                "열심히 참여하겠습니다."
        );

        Application application = applicationService.findApplication(applicationId);

        // then
        assertThat(application.getMember().getId()).isEqualTo(applicant.getId());
        assertThat(application.getStudy().getId()).isEqualTo(study.getId());
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void otherStudyOwnerCannotApproveApplication() {
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
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("스터디 작성자만 신청을 승인하거나 거절할 수 있습니다.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void otherMemberCannotRejectApplication() {
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
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("스터디 작성자만 신청을 승인하거나 거절할 수 있습니다.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void otherMemberCannotCancelApplication() {
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
                .isInstanceOf(ForbiddenOperationException.class)
                .hasMessage("신청자 본인만 신청을 취소할 수 있습니다.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @Test
    void applicantCanCancelOwnApplication() {
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
    void closeStudyApplication() {

        // given
        Member owner = saveTestMember("owner");
        Member firstApplicant = saveTestMember("applicant1");
        Member secondApplicant = saveTestMember("applicant2");
        Study study = saveTestStudy(owner, 1);

        // when
        Long applicationId = applicationService.applyToStudy(
                firstApplicant.getId(),
                study.getId(),
                "신청합니다."
        );

        applicationService.approveApplication(owner.getId(), applicationId);

        // then
        assertThat(study.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);

        assertThatThrownBy(() -> applicationService.applyToStudy(
                secondApplicant.getId(),
                study.getId(),
                "추가 신청합니다."
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("모집이 마감된 스터디입니다.");

    }

    @Test
    void fullCapacity() {
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
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("스터디 모집 정원이 꽉 차서 더 이상 승인할 수 없습니다.");

    }

    @Test
    void sameApplication() {

        // given
        Member owner = saveTestMember("owner");
        Member applicant = saveTestMember("applicant");
        Study study = saveTestStudy(owner, 5);

        // when
        applicationService.applyToStudy(
                applicant.getId(),
                study.getId(),
                "신청합니다."
        );


        // then
        assertThatThrownBy(() -> applicationService.applyToStudy(
                applicant.getId(),
                study.getId(),
                "다시 신청합니다."
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("신청 대기 중이거나 이미 승인된 스터디입니다.");
    }

    @Test
    void findApplicationByStudy() {

        // given
        Member owner = saveTestMember("owner");
        Member firstApplicant = saveTestMember("applicant1");
        Member secondApplicant = saveTestMember("applicant2");
        Study study = saveTestStudy(owner, 5);

        Long firstId = applicationService.applyToStudy(firstApplicant.getId(), study.getId(), "첫 번째 신청");
        Long secondId = applicationService.applyToStudy(secondApplicant.getId(), study.getId(), "두 번째 신청");

        // when
        List<Application> applications = applicationService.findApplicationsByStudy(study.getId());

        // then
        assertThat(applications)
                .extracting(Application::getId)
                .containsExactlyInAnyOrder(firstId, secondId);
    }

    @Test
    void findApplicationsByMember() {

        // given
        Member owner = saveTestMember("owner");
        Member applicant = saveTestMember("applicant");

        Study firstStudy = saveTestStudy(owner, 5);
        Study secondStudy = saveTestStudy(owner, 5);

        Long firstId = applicationService.applyToStudy(applicant.getId(), firstStudy.getId(), "첫 번째 스터디 신청");
        Long secondId = applicationService.applyToStudy(applicant.getId(), secondStudy.getId(), "두 번째 스터디 신청");

        List<Application> applications = applicationService.findApplicationsByMember(applicant.getId());

        assertThat(applications)
                .extracting(Application::getId)
                .containsExactlyInAnyOrder(firstId, secondId);
    }

    @Test
    void deletedStudyApplicationIsNotShownInMemberApplicationList() {
        // given
        Member studyOwner = Member.createMember("studyOwner",
                "1234", "studyOwner@test.com", "오너");

        Member applicant = Member.createMember("applicant", "1234",
                "applicant@test.com", "신청자");

        memberRepository.saveAll(List.of(studyOwner, applicant));

        Study study = Study.createStudy(studyOwner, "삭제될 스터디",
                "내용", StudyMethod.ONLINE, null, 5
        );

        studyRepository.save(study);

        applicationService.applyToStudy(applicant.getId(), study.getId(), "참여하고 싶습니다.");

        studyService.deleteStudy(studyOwner.getId(), study.getId());

        em.flush();
        em.clear();

        //when
        List<Application> applications = applicationService.findApplicationsByMember(applicant.getId());

        //then
        assertThat(applications).isEmpty();
    }

    @Test
    void approvedApplicationCannotBeRejected() {
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
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("대기 중인 신청만 상태를 변경할 수 있습니다.");

        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
    }

    // 로그인 자체를 검사하지 않는 서비스 테스트용 회원
    private Member saveTestMember(String loginId){
        return memberRepository.save(Member.createMember(
                loginId,
                "test-password",
                loginId + "@test.com",
                loginId));
    }

    private Study saveTestStudy(Member owner, int capacity){
        return studyRepository.save(Study.createStudy(
                owner,
                "JPA",
                "JPA를 공부합니다.",
                StudyMethod.ONLINE,
                null,
                capacity
        ));
    }

}