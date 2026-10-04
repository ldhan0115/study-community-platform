package com.study.study_community_platform.service;

import com.study.study_community_platform.config.security.LoginMemberPrincipal;
import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.*;
import com.study.study_community_platform.exception.BusinessRuleException;
import com.study.study_community_platform.repository.ApplicationRepository;
import com.study.study_community_platform.repository.CommentRepository;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class StudyRecruitmentPolicyTest {

    @Autowired
    StudyService studyService;

    @Autowired
    ApplicationService applicationService;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    StudyRepository studyRepository;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    MockMvc mockMvc;

    Member owner;
    Study study;
    int applicantSequence;

    @BeforeEach
    void setUp() {
        applicantSequence = 0;

        owner = memberRepository.save(Member.createMember(
                "studyOwner",
                passwordEncoder.encode("password123"),
                "studyOwner@test.com",
                "스터디방장"
        ));

        study = studyRepository.save(Study.createStudy(
                owner,
                "기존 제목",
                "기존 본문",
                StudyMethod.OFFLINE,
                "서울",
                5
        ));
    }

    @AfterEach
    void cleanUp() {
        // 테스트용 인메모리 DB의 자식 데이터부터 정리
        applicationRepository.deleteAllInBatch();
        commentRepository.deleteAllInBatch();
        studyRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @Test
    void ownerCannotApplyThroughServiceOrHttp() throws Exception {
        long beforeCount = applicationRepository.count();

        assertThatThrownBy(() -> applicationService.applyToStudy(
                owner.getId(),
                study.getId(),
                "방장이 직접 신청합니다."
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("스터디 방장은 자신의 스터디에 신청할 수 없습니다.");

        // 화면에 버튼이 없어도 직접 POST하는 상황을 검증한다.
        mockMvc.perform(ownerPost(
                        "/studies/" + study.getId() + "/apply"
                ).param("message", "방장이 직접 신청합니다."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/studies/" + study.getId()))
                .andExpect(flash().attribute(
                        "errorMessage",
                        "스터디 방장은 자신의 스터디에 신청할 수 없습니다."
                ));

        assertThat(applicationRepository.count()).isEqualTo(beforeCount);
    }

    @Test
    void capacityBelowApprovedCountKeepsAllSavedFields() {
        approveMembers(3);

        assertThatThrownBy(() -> changeCapacity(2))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("현재 승인 인원");

        // 서비스 트랜잭션이 끝난 뒤 DB를 다시 읽어 확인
        assertOriginalStudy();
        assertThat(approvedCount()).isEqualTo(3);
    }

    @Test
    void invalidCapacityIsShownOnEditForm() throws Exception {
        approveMembers(3);

        mockMvc.perform(ownerPost(
                        "/studies/" + study.getId() + "/edit"
                )
                        .param("title", "바뀌면 안 되는 제목")
                        .param("content", "바뀌면 안 되는 본문")
                        .param("method", "ONLINE")
                        .param("region", "")
                        .param("capacity", "2")

                        // 클라이언트가 승인 인원을 조작해 보내도 신뢰x
                        .param("approvedCount", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("studies/editStudyForm"))
                .andExpect(model().attributeHasErrors("editForm"));

        assertOriginalStudy();
        assertThat(approvedCount()).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({
            "3, CLOSED",
            "4, OPEN"
    })
    void capacityBoundaryUpdatesRecruitmentStatus(
            int capacity,
            StudyStatus expectedStatus) {

        approveMembers(3);

        changeCapacity(capacity);

        Study updated = storedStudy();

        assertThat(updated.getCapacity()).isEqualTo(capacity);
        assertThat(updated.getStudyStatus()).isEqualTo(expectedStatus);
        assertThat(approvedCount()).isEqualTo(3);
    }

    @Test
    void fullStudyReopensAfterCapacityIncrease() {
        changeCapacity(1);

        // 대기 신청은 정원에 포함하지 않으므로 두 명 신청 가능
        Long firstApplicationId = applyNewMember();
        Long secondApplicationId = applyNewMember();

        assertThat(approvedCount()).isZero();

        applicationService.approveApplication(
                owner.getId(),
                firstApplicationId
        );

        assertThat(storedStudy().getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
        assertThat(approvedCount()).isEqualTo(1);

        // 정원이 찬 상태에서 남은 신청을 승인 불가
        assertThatThrownBy(() -> applicationService.approveApplication(
                owner.getId(),
                secondApplicationId
        ))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("스터디 모집 정원이 꽉 차서 더 이상 승인할 수 없습니다.");

        assertThat(applicationRepository.findById(secondApplicationId)
                .orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.PENDING);

        long beforeCount = applicationRepository.count();

        // 마감된 스터디에는 새로운 신청도 불가
        assertThatThrownBy(() -> applyNewMember())
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("모집이 마감된 스터디입니다.");

        assertThat(applicationRepository.count()).isEqualTo(beforeCount);

        // 정원을 늘리면 여유가 생기므로 자동으로 모집을 재개
        changeCapacity(2);

        assertThat(storedStudy().getStudyStatus()).isEqualTo(StudyStatus.OPEN);
        assertThat(approvedCount()).isEqualTo(1);

        // 모집 재개 후 새로운 신청 가능
        Long thirdApplicationId = applyNewMember();

        assertThat(applicationRepository.findById(thirdApplicationId)
                .orElseThrow().getStatus())
                .isEqualTo(ApplicationStatus.PENDING);

        // 기존에 대기 중이던 신청도 승인 가능
        applicationService.approveApplication(
                owner.getId(),
                secondApplicationId
        );

        assertThat(approvedCount()).isEqualTo(2);
        assertThat(storedStudy().getStudyStatus()).isEqualTo(StudyStatus.CLOSED);
    }

    @Test
    void domainRejectsCapacityBeforeChangingFields() {
        Study domainStudy = Study.createStudy(
                owner,
                "기존 제목",
                "기존 본문",
                StudyMethod.OFFLINE,
                "서울",
                5
        );

        // DB 롤백에만 기대지 않고 객체 자체가 바뀌지 않는지 확인
        assertThatThrownBy(() -> domainStudy.changeStudyInfo(
                "변경 제목",
                "변경 본문",
                StudyMethod.ONLINE,
                null,
                2,
                3L
        )).isInstanceOf(BusinessRuleException.class);

        assertThat(domainStudy.getTitle()).isEqualTo("기존 제목");
        assertThat(domainStudy.getContent()).isEqualTo("기존 본문");
        assertThat(domainStudy.getMethod()).isEqualTo(StudyMethod.OFFLINE);
        assertThat(domainStudy.getRegion()).isEqualTo("서울");
        assertThat(domainStudy.getCapacity()).isEqualTo(5);
        assertThat(domainStudy.getStudyStatus()).isEqualTo(StudyStatus.OPEN);
    }

    private void changeCapacity(int capacity) {
        studyService.updateStudy(
                owner.getId(),
                study.getId(),
                "변경 제목",
                "변경 본문",
                StudyMethod.ONLINE,
                null,
                capacity
        );
    }

    private Long applyNewMember(){
        applicantSequence++;
        String loginId = "applicant" + applicantSequence;

        Member applicant = memberRepository.save(Member.createMember(
                loginId,
                owner.getPassword(),
                loginId + "@test.com",
                "신청자" + applicantSequence));

        return applicationService.applyToStudy(
                applicant.getId(),
                study.getId(),
                "열심히 참여하겠습니다."
        );
    }

    private void approveMembers(int count){
        for(int i=0; i<count; i++){
            Long applicationId = applyNewMember();

            applicationService.approveApplication(
                    owner.getId(),
                    applicationId
            );
        }
    }

    private long approvedCount(){
        return applicationRepository.countByStudyIdAndStatus(
                study.getId(),
                ApplicationStatus.APPROVED
        );
    }

    private Study storedStudy(){
        return studyRepository.findById(study.getId()).orElseThrow();
    }

    private void assertOriginalStudy(){
        Study actual = storedStudy();

        assertThat(actual.getTitle()).isEqualTo("기존 제목");
        assertThat(actual.getContent()).isEqualTo("기존 본문");
        assertThat(actual.getMethod()).isEqualTo(StudyMethod.OFFLINE);
        assertThat(actual.getRegion()).isEqualTo("서울");
        assertThat(actual.getCapacity()).isEqualTo(5);
        assertThat(actual.getStudyStatus()).isEqualTo(StudyStatus.OPEN);
    }

    private MockHttpServletRequestBuilder ownerPost(String url){
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                SessionConst.LOGIN_MEMBER,
                LoginMemberSession.from(owner)
        );

        return post(url)
                .session(session)
                .with(user(LoginMemberPrincipal.from(owner)))
                .with(csrf());
    }
}
