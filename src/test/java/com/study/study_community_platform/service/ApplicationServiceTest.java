package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.*;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ApplicationServiceTest {

    @Autowired ApplicationService applicationService;
    @Autowired MemberRepository memberRepository;
    @Autowired StudyRepository studyRepository;

    @Test
    void applyToStudy(){
        // given
        Member member = new Member("test", "1234", "test@gmail.com", "tester");
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
    void closeStudyApplication(){
        // given
        Member member = new Member("test", "1234", "test@gmail.com", "tester");
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
        Member member1 = new Member("test1", "1234", "test1@gmail.com", "tester1");
        Member member2 = new Member("test2", "1234", "test2@gmail.com", "tester2");
        memberRepository.save(member1);
        memberRepository.save(member2);

        Study study = Study.createStudy(member1, "JPA", "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 1);
        studyRepository.save(study);

        applicationService.applyToStudy(member1.getId(), study.getId(), "열심히 하겠습니다.");

        //when
        assertThatThrownBy(() -> applicationService.applyToStudy(member2.getId(), study.getId(), "저도 열심히 할게요"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("스터디 정원이 가득 찼습니다.");

    }

    @Test
    void sameApplication(){
        // given
        Member member = new Member("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Study study = Study.createStudy(member, "JPA", "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 10);
        studyRepository.save(study);

        applicationService.applyToStudy(member.getId(), study.getId(), "열심히 하겠습니다.");

        //when
        assertThatThrownBy(() -> applicationService.applyToStudy(member.getId(), study.getId(), "또 왔어요"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 신청한 스터디입니다.");
    }

    @Test
    void findApplicationByStudy(){
        // given
        Member member1 = new Member("test1", "1234", "test1@gmail.com", "tester1");
        Member member2 = new Member("test2", "1234", "test2@gmail.com", "tester2");
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
        Member member = new Member("test1", "1234", "test1@gmail.com", "tester1");
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
    void changeApplicationStatus(){
        // given
        Member member = new Member("test1", "1234", "test1@gmail.com", "tester1");
        memberRepository.save(member);

        Study study = Study.createStudy(member, "JPA", "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 10);
        studyRepository.save(study);

        Long applicationId = applicationService.applyToStudy(member.getId(), study.getId(), "화이팅");
        Application findApplication = applicationService.findApplication(applicationId);

        //when && then

        applicationService.approveApplication(applicationId);
        assertThat(findApplication.getStatus()).isEqualTo(ApplicationStatus.APPROVED);

        applicationService.rejectApplication(applicationId);
        assertThat(findApplication.getStatus()).isEqualTo(ApplicationStatus.REJECTED);
    }

}