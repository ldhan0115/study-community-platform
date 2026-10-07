package com.study.study_community_platform.controller;

import com.study.study_community_platform.config.security.LoginMemberPrincipal;
import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.*;
import com.study.study_community_platform.exception.BusinessRuleException;
import com.study.study_community_platform.exception.ForbiddenOperationException;
import com.study.study_community_platform.exception.ResourceNotFoundException;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import com.study.study_community_platform.service.ApplicationService;
import com.study.study_community_platform.service.CommentService;
import com.study.study_community_platform.service.StudyService;
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
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.*;
import static org.springframework.security.web.context.HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// 요청별 커밋/롤백을 확인하기 위해 클래스에 @Transactional을 붙이지 않음
class SecurityHttpFlowTest {

    @Autowired MockMvc mockMvc;
    @Autowired MemberRepository memberRepository;
    @Autowired StudyRepository studyRepository;
    @Autowired ApplicationService applicationService;
    @Autowired CommentService commentService;
    @Autowired StudyService studyService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;

    Member owner;
    Member applicant;
    Member outsider;

    Study study;
    Study outsiderStudy;

    Long applicationId;
    Long commentId;

    @BeforeEach
    void setUp() {
        String hash = passwordEncoder.encode("password123");

        owner = saveMember("owner", hash);
        applicant = saveMember("applicant", hash);
        outsider = saveMember("outsider", hash);

        study = saveStudy(owner);
        outsiderStudy = saveStudy(outsider);

        applicationId = applicationService.applyToStudy(
                applicant.getId(),
                study.getId(),
                "참여하겠습니다."
        );

        commentId = commentService.registerComment(
                applicant.getId(),
                study.getId(),
                "원래 댓글"
        );
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from application");
        jdbc.update("delete from comment");
        jdbc.update("delete from study");
        jdbc.update("delete from member");
    }

    @Test
    void realLoginSessionCanCreateComment() throws Exception {
        MockHttpSession session = login(applicant);

        int before = jdbc.queryForObject(
                "select count(*) from comment",
                Integer.class
        );

        mockMvc.perform(
                        post("/studies/{id}/comments", study.getId())
                                .session(session)
                                .with(csrf())
                                .param("content", "로그인 후 작성한 댓글")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/studies/" + study.getId()))
                .andExpect(authenticated().withUsername(applicant.getLoginId()));

        assertThat(jdbc.queryForObject(
                "select count(*) from comment",
                Integer.class
        )).isEqualTo(before + 1);

        assertThat(jdbc.queryForObject("""
                select count(*) from comment
                where member_id = ? and study_id = ?
                and cast(content as varchar(1000)) = ?
                """,
                Integer.class,
                applicant.getId(),
                study.getId(),
                "로그인 후 작성한 댓글"
        )).isEqualTo(1);
    }

    @Test
    void wrongPasswordDoesNotAuthenticate() throws Exception {
        var before = databaseSnapshot();

        expectLoginFailure(
                applicant.getLoginId(),
                "wrongPassword"
        );

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @Test
    void logoutInvalidatesSessionAndBlocksProtectedRequests() throws Exception {
        MockHttpSession session = login(applicant);
        var before = databaseSnapshot();

        mockMvc.perform(
                        post("/members/logout")
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(unauthenticated());

        assertThat(session.isInvalid()).isTrue();

        // 무효화된 MockHttpSession을 다시 전달하지 않고 미인증 요청으로 확인
        mockMvc.perform(get("/members/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login"))
                .andExpect(unauthenticated());

        // 유효한 CSRF 토큰을 보내 인증 부족에 의한 차단인지 구분
        mockMvc.perform(
                        post("/studies/{id}/comments", study.getId())
                                .with(csrf())
                                .param("content", "저장되면 안 되는 댓글")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login"))
                .andExpect(unauthenticated());

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingOrInvalidCsrfTokenIsRejected(
            boolean invalidToken
    ) throws Exception {
        MockHttpSession session = login(owner);
        var before = databaseSnapshot();

        var request = post("/studies/{id}/delete", study.getId())
                .session(session);

        if (invalidToken) {
            request.with(csrf().useInvalidToken());
        }

        mockMvc.perform(request)
                .andExpect(status().isForbidden())
                // CSRF 필터에서 막혀 컨트롤러까지 도달하지 않음
                .andExpect(result ->
                        assertThat(result.getHandler()).isNull()
                );

        assertThat(session.isInvalid()).isFalse();
        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @Test
    void successfulMemberEditRequiresLoginWithUpdatedCredentials()
            throws Exception {
        MockHttpSession oldSession = login(applicant);
        long beforeCount = memberRepository.count();

        mockMvc.perform(memberEdit(oldSession, "updated@test.com"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login?updated"))
                .andExpect(unauthenticated());

        assertThat(oldSession.isInvalid()).isTrue();

        Member updated = memberRepository.findById(applicant.getId())
                .orElseThrow();

        assertThat(memberRepository.count()).isEqualTo(beforeCount);
        assertThat(updated.getId()).isEqualTo(applicant.getId());
        assertThat(updated.getLoginId()).isEqualTo("updatedUser");
        assertThat(updated.getEmail()).isEqualTo("updated@test.com");
        assertThat(updated.getNickname()).isEqualTo("수정회원");

        assertThat(passwordEncoder.matches(
                "newPassword123",
                updated.getPassword()
        )).isTrue();

        // 이전 아이디 및 이전 비밀번호로는 로그인할 수 없음
        expectLoginFailure(applicant.getLoginId(), "password123");
        expectLoginFailure("updatedUser", "password123");

        MockHttpSession newSession = login(
                "updatedUser",
                "newPassword123"
        );

        // 같은 회원 ID로 최신 Principal과 화면용 DTO가 생성됨
        LoginMemberPrincipal principal = principal(newSession);

        assertThat(principal.getMemberId()).isEqualTo(applicant.getId());
        assertThat(principal.getUsername()).isEqualTo("updatedUser");
        assertThat(principal.getNickname()).isEqualTo("수정회원");

        assertThat(newSession.getAttribute(SessionConst.LOGIN_MEMBER))
                .isEqualTo(
                        new LoginMemberSession(applicant.getId(), "수정회원")
                );

        mockMvc.perform(get("/members/edit").session(newSession))
                .andExpect(status().isOk())
                .andExpect(authenticated().withUsername("updatedUser"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "owner@test.com"})
    void failedMemberEditKeepsDatabaseAndAuthentication(
            String email
    ) throws Exception {
        MockHttpSession session = login(applicant);
        var before = databaseSnapshot();

        mockMvc.perform(memberEdit(session, email))
                .andExpect(status().isOk())
                .andExpect(view().name("members/editMemberForm"))
                .andExpect(model().attributeHasFieldErrors("member", "email"))
                .andExpect(authenticated().withUsername(applicant.getLoginId()));

        assertThat(session.isInvalid()).isFalse();

        assertThat(principal(session).getUsername())
                .isEqualTo(applicant.getLoginId());

        assertThat(principal(session).getNickname())
                .isEqualTo(applicant.getNickname());

        assertThat(session.getAttribute(SessionConst.LOGIN_MEMBER))
                .isEqualTo(LoginMemberSession.from(applicant));

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "studyEdit",
            "studyDelete",
            "approve",
            "reject",
            "cancel",
            "commentEdit",
            "commentDelete"
    })
    void anotherMemberCannotModifyResources(
            String operation
    ) throws Exception {
        MockHttpSession session = login(outsider);
        var before = databaseSnapshot();

        mockMvc.perform(
                        unauthorizedRequest(operation)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/error"))
                .andExpect(result ->
                        assertThat(result.getResolvedException())
                                .isInstanceOf(ForbiddenOperationException.class)
                );

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void missingOrDeletedApplicationReturns404(
            boolean deletedStudy
    ) throws Exception {
        MockHttpSession session = login(owner);

        if (deletedStudy) {
            studyService.deleteStudy(owner.getId(), study.getId());
        }

        long targetId = deletedStudy ? applicationId : Long.MAX_VALUE;
        var before = databaseSnapshot();

        mockMvc.perform(
                        post("/applications/{id}/approve", targetId)
                                .session(session)
                                .with(csrf())
                )
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/error"))
                .andExpect(result ->
                        assertThat(result.getResolvedException())
                                .isInstanceOf(ResourceNotFoundException.class)
                );

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"approve", "reject", "cancel"})
    void approvedApplicationCannotBeChangedAgain(
            String operation
    ) throws Exception {
        MockHttpSession ownerSession = login(owner);
        MockHttpSession applicantSession = login(applicant);

        mockMvc.perform(applicationAction("approve", ownerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl("/studies/" + study.getId() + "/applicants")
                );

        assertThat(applicationStatus(applicationId)).isEqualTo("APPROVED");
        var before = databaseSnapshot();

        // 각 작업의 정당한 권한자로 요청하여 상태 규칙 위반을 검증
        MockHttpSession session = operation.equals("cancel")
                ? applicantSession
                : ownerSession;

        mockMvc.perform(applicationAction(operation, session))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error/error"))
                .andExpect(result ->
                        assertThat(result.getResolvedException())
                                .isInstanceOf(BusinessRuleException.class)
                );

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancel", "reject"})
    void canceledOrRejectedApplicationCanBeSubmittedAgain(
            String operation
    ) throws Exception {
        MockHttpSession applicantSession = login(applicant);

        MockHttpSession actorSession = operation.equals("cancel")
                ? applicantSession
                : login(owner);

        String expectedStatus = operation.equals("cancel")
                ? "CANCELED"
                : "REJECTED";

        mockMvc.perform(applicationAction(operation, actorSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        redirectedUrl(
                                operation.equals("cancel")
                                        ? "/applications"
                                        : "/studies/" + study.getId() + "/applicants"
                        )
                );

        assertThat(applicationStatus(applicationId))
                .isEqualTo(expectedStatus);

        mockMvc.perform(
                        post("/studies/{id}/apply", study.getId())
                                .session(applicantSession)
                                .with(csrf())
                                .param("message", "다시 신청합니다.")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/studies/" + study.getId()))
                .andExpect(flash().attributeExists("successMessage"));

        // 기존 신청 이력은 유지
        assertThat(applicationStatus(applicationId))
                .isEqualTo(expectedStatus);

        assertThat(jdbc.queryForObject("""
                select count(*) from application
                where member_id = ? and study_id = ?
                """,
                Integer.class,
                applicant.getId(),
                study.getId()
        )).isEqualTo(2);

        // 기존 신청의 상태를 되돌리는 대신 새 PENDING 신청을 생성
        assertThat(jdbc.queryForObject("""
                select count(*) from application
                where member_id = ? and study_id = ?
                and application_id <> ?
                and application_status = 'PENDING'
                """,
                Integer.class,
                applicant.getId(),
                study.getId(),
                applicationId
        )).isEqualTo(1);
    }

    @Test
    void duplicateApplicationShowsFlashErrorWithoutSaving()
            throws Exception {
        MockHttpSession session = login(applicant);
        var before = databaseSnapshot();

        mockMvc.perform(
                        post("/studies/{id}/apply", study.getId())
                                .session(session)
                                .with(csrf())
                                .param("message", "중복 신청")
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/studies/" + study.getId()))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("applicationDraft", "중복 신청"));

        assertThat(databaseSnapshot()).isEqualTo(before);
    }

    private Member saveMember(String loginId, String hash) {
        return memberRepository.save(
                Member.createMember(
                        loginId,
                        hash,
                        loginId + "@test.com",
                        loginId
                )
        );
    }

    private Study saveStudy(Member member) {
        return studyRepository.save(
                Study.createStudy(
                        member,
                        "스터디 제목",
                        "스터디 소개",
                        StudyMethod.ONLINE,
                        null,
                        5
                )
        );
    }

    private MockHttpSession login(Member member) throws Exception {
        return login(member.getLoginId(), "password123");
    }

    private MockHttpSession login(
            String loginId,
            String password
    ) throws Exception {
        // user()로 인증을 주입하지 않고 로그인 필터가 만든 세션을 반환
        return (MockHttpSession) mockMvc.perform(
                        post("/members/login")
                                .with(csrf())
                                .param("loginId", loginId)
                                .param("password", password)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(authenticated().withUsername(loginId))
                .andReturn()
                .getRequest()
                .getSession(false);
    }

    private void expectLoginFailure(
            String loginId,
            String password
    ) throws Exception {
        mockMvc.perform(
                        post("/members/login")
                                .with(csrf())
                                .param("loginId", loginId)
                                .param("password", password)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login?error"))
                .andExpect(unauthenticated());
    }

    private LoginMemberPrincipal principal(MockHttpSession session) {
        SecurityContext context = (SecurityContext)
                session.getAttribute(SPRING_SECURITY_CONTEXT_KEY);

        assertThat(context).isNotNull();

        return (LoginMemberPrincipal)
                context.getAuthentication().getPrincipal();
    }

    private MockHttpServletRequestBuilder memberEdit(
            MockHttpSession session,
            String email
    ) {
        return post("/members/edit")
                .session(session)
                .with(csrf())
                .param("loginId", "updatedUser")
                .param("password", "newPassword123")
                .param("email", email)
                .param("nickname", "수정회원");
    }

    private MockHttpServletRequestBuilder applicationAction(
            String operation,
            MockHttpSession session
    ) {
        return post(
                "/applications/{id}/{operation}",
                applicationId,
                operation
        )
                .session(session)
                .with(csrf());
    }

    private MockHttpServletRequestBuilder unauthorizedRequest(
            String operation
    ) {
        return switch (operation) {
            case "studyEdit" ->
                    post("/studies/{id}/edit", study.getId())
                            .param("title", "변경 시도")
                            .param("content", "변경 시도")
                            .param("method", "ONLINE")
                            .param("capacity", "5");

            case "studyDelete" ->
                    post("/studies/{id}/delete", study.getId());

            case "approve", "reject" ->
                    post(
                            "/applications/{id}/{operation}",
                            applicationId,
                            operation
                    )
                            // 자신의 다른 스터디 ID를 보내도
                            // 실제 신청과 연결된 스터디의 작성자를 검사해야 함
                            .param("studyId", outsiderStudy.getId().toString());

            case "cancel" ->
                    post("/applications/{id}/cancel", applicationId);

            case "commentEdit" ->
                    post(
                            "/studies/{id}/comments/{commentId}/edit",
                            study.getId(),
                            commentId
                    ).param("content", "변경 시도");

            case "commentDelete" ->
                    post(
                            "/studies/{id}/comments/{commentId}/delete",
                            study.getId(),
                            commentId
                    );

            default -> throw new IllegalArgumentException(operation);
        };
    }

    private String applicationStatus(Long id) {
        return jdbc.queryForObject(
                "select application_status from application where application_id = ?",
                String.class,
                id
        );
    }

    private List<List<Map<String, Object>>> databaseSnapshot() {
        // JPA 캐시와 논리 삭제 필터의 영향을 받지 않는 DB 값 비교
        // CLOB은 문자열로 변환하여 객체 참조 대신 내용 자체를 비교
        return List.of(
                jdbc.queryForList("""
                        select *
                        from member
                        order by member_id
                        """),

                jdbc.queryForList("""
                        select study_id, member_id, study_title,
                               cast(study_content as varchar(10000)) as content,
                               method, region, capacity, study_status,
                               deleted_at, created_at, updated_at
                        from study
                        order by study_id
                        """),

                jdbc.queryForList("""
                        select *
                        from application
                        order by application_id
                        """),

                jdbc.queryForList("""
                        select comment_id, member_id, study_id,
                               cast(content as varchar(1000)) as content,
                               deleted_at, created_at, updated_at
                        from comment
                        order by comment_id
                        """)
        );
    }
}