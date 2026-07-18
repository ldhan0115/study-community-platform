package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.domain.StudyStatus;
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
class StudyServiceTest {

    @Autowired StudyService studyService;
    @Autowired EntityManager em;

    @Test
    void createStudy(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        em.persist(member);

        //when
        Long studyId = studyService.registerStudy(member.getId(), "JPA",
                "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 5);

        Study findStudy = studyService.findStudy(studyId);

        //then
        assertThat(findStudy.getContent()).isEqualTo("JPA를 열심히 공부해요");
        assertThat(findStudy.getStudyStatus()).isEqualTo(StudyStatus.OPEN);
    }

    @Test
    void studyNoExist(){
        //then
        assertThatThrownBy(() -> studyService.findStudy(10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 스터디입니다.");

    }

    @Test
    void noTitle(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        em.persist(member);

        //when&then
        assertThatThrownBy(() -> studyService.registerStudy(member.getId(), null,
                "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스터디 제목은 필수입니다.");

    }

    @Test
    void capacityException(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        em.persist(member);

        //when&then
        assertThatThrownBy(() -> studyService.registerStudy(member.getId(), "JPA",
                "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("모집 인원은 1명 이상이어야 합니다.");
    }

    @Test
    void findStudies(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        em.persist(member);

        Long studyId1 = studyService.registerStudy(member.getId(), "JPA",
                "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 5);

        Long studyId2 = studyService.registerStudy(member.getId(), "SPRING",
                "SPRING을 열심히 공부해요", StudyMethod.OFFLINE, "서울", 5);

        Study findStudy1 = studyService.findStudy(studyId1);
        Study findStudy2 = studyService.findStudy(studyId2);

        //when
        List<Study> studies = studyService.findStudies();

        //then
        assertThat(studies.size()).isEqualTo(2);
        assertThat(studies).contains(findStudy1, findStudy2);
    }

    @Test
    void updateStudy(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        em.persist(member);

        Long studyId = studyService.registerStudy(member.getId(), "JPA",
                "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 5);

        Study updatedStudy = Study.createStudy(member, "DB",
                "DB를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 5);

        //when
        studyService.updateStudy(studyId, updatedStudy.getTitle(), updatedStudy.getContent(), updatedStudy.getMethod()
                , updatedStudy.getRegion(), updatedStudy.getCapacity());
        Study findStudy = studyService.findStudy(studyId);

        //then
        assertThat(updatedStudy.getTitle()).isEqualTo(findStudy.getTitle());
        assertThat(updatedStudy.getContent()).isEqualTo(findStudy.getContent());
        assertThat(updatedStudy.getMethod()).isEqualTo(findStudy.getMethod());
        assertThat(updatedStudy.getCapacity()).isEqualTo(findStudy.getCapacity());
    }

    @Test
    void closeStudy(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        em.persist(member);

        Long studyId = studyService.registerStudy(member.getId(), "JPA",
                "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 5);

        Study findStudy = studyService.findStudy(studyId);
        //when
        studyService.closeStudy(studyId);

        //then
        assertThat(findStudy.getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
    }

    @Test
    void deleteStudy(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        em.persist(member);

        Long studyId1 = studyService.registerStudy(member.getId(), "JPA",
                "JPA를 열심히 공부해요", StudyMethod.OFFLINE, "서울", 5);

        Long studyId2 = studyService.registerStudy(member.getId(), "SPRING",
                "SPRING을 열심히 공부해요", StudyMethod.OFFLINE, "서울", 5);

        //when
        studyService.deleteStudy(studyId1);
        List<Study> studies = studyService.findStudies();

        //then
        assertThat(studies.size()).isEqualTo(1);
    }
}