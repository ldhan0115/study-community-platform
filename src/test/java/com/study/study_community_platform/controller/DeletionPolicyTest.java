package com.study.study_community_platform.controller;

import com.study.study_community_platform.config.security.LoginMemberPrincipal;
import com.study.study_community_platform.domain.*;
import com.study.study_community_platform.exception.ForbiddenOperationException;
import com.study.study_community_platform.exception.ResourceNotFoundException;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import com.study.study_community_platform.service.*;
import com.study.study_community_platform.service.dto.MemberUpdateDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeletionPolicyTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired MemberService memberService;
    @Autowired StudyService studyService;
    @Autowired ApplicationService applicationService;
    @Autowired CommentService commentService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    Member owner;
    Member applicant;
    Member outsider;

    Study study;
    Study anotherStudy;

    Long applicationId;
    Long commentId;
    Long ownerCommentId;

    @BeforeEach
    void setUp() {
        String hash = passwordEncoder.encode("password123");

        owner = saveMember("owner", hash);
        applicant = saveMember("applicant", hash);
        outsider = saveMember("outsider", hash);

        study = studyRepository.save(Study.createStudy(
                owner,
                "원래 제목",
                "원래 소개",
                StudyMethod.ONLINE,
                null,
                5
        ));

        anotherStudy = studyRepository.save(Study.createStudy(
                outsider,
                "다른 스터디",
                "다른 소개",
                StudyMethod.ONLINE,
                null,
                5
        ));

        applicationId = applicationService.applyToStudy(
                applicant.getId(),
                study.getId(),
                "참여하겠습니다."
        );

        commentId = commentService.registerComment(
                applicant.getId(),
                study.getId(),
                "신청자의 원래 댓글"
        );

        ownerCommentId = commentService.registerComment(
                owner.getId(),
                study.getId(),
                "방장의 원래 댓글"
        );
    }

    @AfterEach
    void cleanUp() {
        // test 프로필의 H2에서 논리 삭제된 행까지 정리
        // 외래 키가 있는 자식 데이터부터 삭제
        jdbc.update("delete from application");
        jdbc.update("delete from comment");
        jdbc.update("delete from study");
        jdbc.update("delete from member");
    }

    @ParameterizedTest
    @ValueSource(strings = {"study", "application", "comment"})
    void withdrawalBlocksAnotherRealLoginSession(
            String operation
    ) throws Exception {
        MockHttpSession first = login(applicant);
        MockHttpSession second = login(applicant);

        assertThat(first.getId()).isNotEqualTo(second.getId());

        mockMvc.perform(
                        post("/members/withdraw")
                                .session(first)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(unauthenticated());

        assertThat(first.isInvalid()).isTrue();

        // 두 번째 세션은 아직 요청하지 않았으므로 남아 있음
        assertThat(second.isInvalid()).isFalse();

        assertThat(
                memberRepository.findById(applicant.getId())
                        .orElseThrow()
                        .getDeletedAt()
        ).isNotNull();

        var before = databaseSnapshot();

        MockHttpServletRequestBuilder request = switch (operation) {
            case "study" -> studyForm("/studies/new");

            case "application" ->
                    post("/studies/{id}/apply", anotherStudy.getId())
                            .param("message", "새 신청");

            case "comment" ->
                    post("/studies/{id}/comments", study.getId())
                            .param("content", "새 댓글");

            default -> throw new IllegalArgumentException(operation);
        };

        // user()로 인증을 주입하지 않고 실제 로그인 세션을 재사용
        mockMvc.perform(
                        request.session(second).with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login?withdrawn"))
                .andExpect(unauthenticated());

        assertThat(second.isInvalid()).isTrue();
        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "studyCreate",
            "studyUpdate",
            "studyDelete",
            "apply",
            "approve",
            "reject",
            "cancel",
            "commentCreate",
            "commentUpdate",
            "commentDelete",
            "memberUpdate",
            "withdraw"
    })
    void withdrawnMemberCannotWriteThroughServices(String operation) {
        Member actor = operation.equals("cancel") ? applicant : owner;

        memberService.withdrawMember(actor.getId());

        var before = databaseSnapshot();

        // HTTP 필터를 거치지 않는 호출도 차단되어야함
        assertThatThrownBy(() -> invokeWrite(operation, actor))
                .isInstanceOf(ForbiddenOperationException.class);

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "apply",
            "approve",
            "reject",
            "cancel",
            "commentCreate",
            "commentUpdate",
            "commentDelete",
            "studyUpdate",
            "studyDelete"
    })
    void deletedStudyRejectsWriteRequests(String operation) throws Exception {
        studyService.deleteStudy(owner.getId(), study.getId());

        assertThat(studyRepository.findById(study.getId())).isEmpty();

        var before = databaseSnapshot();

        // 유효한 폼 값을 보내 실제 리소스 검증까지 도달
        mockMvc.perform(writeRequest(operation))
                .andExpect(status().isNotFound());

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject", "cancel"})
    void deletedStudyRejectsApplicationServiceCalls(String operation) {
        studyService.deleteStudy(owner.getId(), study.getId());

        Member actor = operation.equals("cancel") ? applicant : owner;
        var before = databaseSnapshot();

        // Controller의 사전 조회 없이 호출해도 삭제된 부모 검사
        assertThatThrownBy(() -> invokeWrite(operation, actor))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"edit", "delete"})
    void commentUrlMustMatchItsActualStudy(String action) throws Exception {
        var before = databaseSnapshot();

        // 작성자 본인이라도 다른 스터디의 URL로 댓글을 변경할 수 없음
        mockMvc.perform(asMember(
                        applicant,
                        post(
                                "/studies/{studyId}/comments/{commentId}/{action}",
                                anotherStudy.getId(),
                                commentId,
                                action
                        ).param("content", "변경 시도")
                ))
                .andExpect(status().isNotFound());

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"edit", "delete"})
    void anotherMemberCannotChangeComment(String action) throws Exception {
        var before = databaseSnapshot();

        mockMvc.perform(asMember(
                        outsider,
                        post(
                                "/studies/{studyId}/comments/{commentId}/{action}",
                                study.getId(),
                                commentId,
                                action
                        ).param("content", "변경 시도")
                ))
                .andExpect(status().isForbidden());

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"edit", "delete"})
    void deletedCommentCannotBeChangedThroughOldUrl(
            String action
    ) throws Exception {
        commentService.deleteComment(
                applicant.getId(),
                study.getId(),
                commentId
        );

        var before = databaseSnapshot();

        mockMvc.perform(asMember(
                        applicant,
                        post(
                                "/studies/{studyId}/comments/{commentId}/{action}",
                                study.getId(),
                                commentId,
                                action
                        ).param("content", "변경 시도")
                ))
                .andExpect(status().isNotFound());

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @Test
    void historicalCommentAuthorRemainsReadableAfterWithdrawal()
            throws Exception {
        memberService.withdrawMember(applicant.getId());

        // 탈퇴한 작성자의 기존 댓글을 포함한 공개 상세 화면 정상 렌더링
        mockMvc.perform(get("/studies/{id}", study.getId()))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(containsString(applicant.getNickname()))
                );
    }

    private Member saveMember(String loginId, String hash) {
        return memberRepository.save(Member.createMember(
                loginId,
                hash,
                loginId + "@test.com",
                loginId
        ));
    }

    private MockHttpSession login(Member member) throws Exception {
        var result = mockMvc.perform(
                        post("/members/login")
                                .with(csrf())
                                .param("loginId", member.getLoginId())
                                .param("password", "password123")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        authenticated().withUsername(member.getLoginId())
                )
                .andReturn();

        MockHttpSession session =
                (MockHttpSession) result.getRequest().getSession(false);

        assertThat(session).isNotNull();

        return session;
    }

    private MockHttpServletRequestBuilder asMember(
            Member member,
            MockHttpServletRequestBuilder request
    ) {
        return request
                .with(user(LoginMemberPrincipal.from(member)))
                .with(csrf());
    }

    private MockHttpServletRequestBuilder studyForm(String path) {
        return post(path)
                .param("title", "새 제목")
                .param("content", "새 소개")
                .param("method", "ONLINE")
                .param("region", "")
                .param("capacity", "5");
    }

    private MockHttpServletRequestBuilder writeRequest(String operation) {
        return switch (operation) {
            case "apply" -> asMember(
                    outsider,
                    post("/studies/{id}/apply", study.getId())
                            .param("message", "새 신청")
            );

            case "approve", "reject" -> asMember(
                    owner,
                    post("/applications/{id}/{action}", applicationId, operation)
            );

            case "cancel" -> asMember(
                    applicant,
                    post("/applications/{id}/cancel", applicationId)
            );

            case "commentCreate" -> asMember(
                    applicant,
                    post("/studies/{id}/comments", study.getId())
                            .param("content", "새 댓글")
            );

            case "commentUpdate" -> asMember(
                    applicant,
                    post(
                            "/studies/{studyId}/comments/{commentId}/edit",
                            study.getId(),
                            commentId
                    ).param("content", "수정 댓글")
            );

            case "commentDelete" -> asMember(
                    applicant,
                    post(
                            "/studies/{studyId}/comments/{commentId}/delete",
                            study.getId(),
                            commentId
                    )
            );

            case "studyUpdate" -> asMember(
                    owner,
                    studyForm("/studies/" + study.getId() + "/edit")
            );

            case "studyDelete" -> asMember(
                    owner,
                    post("/studies/{id}/delete", study.getId())
            );

            default -> throw new IllegalArgumentException(operation);
        };
    }

    private void invokeWrite(String operation, Member actor) {
        Long id = actor.getId();

        switch (operation) {
            case "studyCreate" -> studyService.registerStudy(
                    id, "새 제목", "새 소개", StudyMethod.ONLINE, null, 5
            );

            case "studyUpdate" -> studyService.updateStudy(
                    id, study.getId(),
                    "새 제목", "새 소개", StudyMethod.ONLINE, null, 5
            );

            case "studyDelete" ->
                    studyService.deleteStudy(id, study.getId());

            case "apply" ->
                    applicationService.applyToStudy(id, anotherStudy.getId(), "새 신청");

            case "approve" ->
                    applicationService.approveApplication(id, applicationId);

            case "reject" ->
                    applicationService.rejectApplication(id, applicationId);

            case "cancel" ->
                    applicationService.cancelApplication(id, applicationId);

            case "commentCreate" ->
                    commentService.registerComment(id, study.getId(), "새 댓글");

            case "commentUpdate" ->
                    commentService.updateComment(
                            id, study.getId(), ownerCommentId, "수정 댓글"
                    );

            case "commentDelete" ->
                    commentService.deleteComment(
                            id, study.getId(), ownerCommentId
                    );

            case "memberUpdate" ->
                    memberService.editMember(id, new MemberUpdateDto(
                            "updatedOwner",
                            "password456",
                            "updated@test.com",
                            "변경 닉네임"
                    ));

            case "withdraw" ->
                    memberService.withdrawMember(id);

            default -> throw new IllegalArgumentException(operation);
        }
    }

    private List<List<Map<String, Object>>> databaseSnapshot() {
        // JPA의 조회 제한과 1차 캐시를 거치지 않고 실제 저장값을 비교
        // H2의 CLOB은 문자열로 변환해 내용으로 비교
        return List.of(
                jdbc.queryForList("""
                        select member_id, login_id, password, email, nickname, deleted_at
                        from member
                        order by member_id
                        """),

                jdbc.queryForList("""
                        select study_id, member_id, study_title,
                               cast(study_content as varchar(10000)) as content,
                               method, region, capacity, study_status, deleted_at
                        from study
                        order by study_id
                        """),

                jdbc.queryForList("""
                        select application_id, member_id, study_id, message,
                               application_status
                        from application
                        order by application_id
                        """),

                jdbc.queryForList("""
                        select comment_id, member_id, study_id,
                               cast(content as varchar(1000)) as content,
                               deleted_at
                        from comment
                        order by comment_id
                        """)
        );
    }
}