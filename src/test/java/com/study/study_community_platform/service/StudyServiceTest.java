package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.domain.StudyStatus;
import jakarta.persistence.EntityManager;
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
    void otherMemberCannotFindStudyForOwner(){
        // given
        Member owner = Member.createMember("owner", "1234", "owner@test.com", "오너");
        Member other = Member.createMember("other", "1234", "other@test.com", "아더");

        em.persist(owner);
        em.persist(other);

        Long studyId = studyService.registerStudy(owner.getId(), "titleForOwner",
                "content", StudyMethod.ONLINE, null, 5);

        // when & then
        assertThatThrownBy(() ->
                studyService.findStudyForOwner(other.getId(), studyId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("스터디 작성자만 수정하거나 삭제할 수 있습니다.");
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
        studyService.updateStudy(member.getId(), studyId, updatedStudy.getTitle(), updatedStudy.getContent(), updatedStudy.getMethod()
                , updatedStudy.getRegion(), updatedStudy.getCapacity());
        Study findStudy = studyService.findStudy(studyId);

        //then
        assertThat(updatedStudy.getTitle()).isEqualTo(findStudy.getTitle());
        assertThat(updatedStudy.getContent()).isEqualTo(findStudy.getContent());
        assertThat(updatedStudy.getMethod()).isEqualTo(findStudy.getMethod());
        assertThat(updatedStudy.getCapacity()).isEqualTo(findStudy.getCapacity());
    }

    @Test
    void otherMemberCannotUpdateStudy(){
        // given
        Member owner = Member.createMember("owner", "1234", "owner@test.com", "오너");
        Member other = Member.createMember("other", "1234", "other@test.com", "아더");

        em.persist(owner);
        em.persist(other);

        Long studyId = studyService.registerStudy(owner.getId(), "title",
                "content", StudyMethod.ONLINE, null, 5);

        //when & then
        assertThatThrownBy(() ->
                studyService.updateStudy(other.getId(), studyId, "otherTitle",
                        "otherContent", StudyMethod.ONLINE, null, 10))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("스터디 작성자만 수정하거나 삭제할 수 있습니다.");

        em.clear();

        Study unchangedStudy = studyService.findStudy(studyId);

        assertThat(unchangedStudy.getTitle()).isEqualTo("title");
        assertThat(unchangedStudy.getContent()).isEqualTo("content");
        assertThat(unchangedStudy.getCapacity()).isEqualTo(5);
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
        studyService.deleteStudy(member.getId(), studyId1);
        List<Study> studies = studyService.findStudies();

        //then
        assertThat(studies.size()).isEqualTo(1);
    }

    @Test
    void otherMemberCannotDeleteStudy(){
        // given
        Member owner = Member.createMember("owner", "1234", "owner@test.com", "오너");
        Member other = Member.createMember("other", "1234", "other@test.com", "아더");

        em.persist(owner);
        em.persist(other);

        Long studyId = studyService.registerStudy(owner.getId(), "No delete", "content",
                StudyMethod.ONLINE, null, 5);

        //when & then
        assertThatThrownBy(() ->
                studyService.deleteStudy(other.getId(), studyId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("스터디 작성자만 수정하거나 삭제할 수 있습니다.");

        em.clear();

        Study study = studyService.findStudy(studyId);
        assertThat(study.getDeletedAt()).isNull();
    }
}