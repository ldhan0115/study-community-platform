package com.study.study_community_platform.controller;

import com.study.study_community_platform.config.security.LoginMemberPrincipal;
import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.Application;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.exception.BusinessRuleException;
import com.study.study_community_platform.repository.ApplicationRepository;
import com.study.study_community_platform.repository.CommentRepository;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.validation.BindingResult;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ValidationTest {

    @Autowired
    MockMvc mockMvc;

    // 대부분은 실제 저장소로 동작
    // DB 충돌 테스트에서 사전 중복 조회 결과를 바꿀 때 사용
    @MockitoSpyBean
    MemberRepository memberRepository;

    @Autowired
    StudyRepository studyRepository;

    @Autowired
    ApplicationRepository applicationRepository;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    Member member;
    Member other;
    Study study;
    MockHttpSession session;

    long originalMemberCount;
    long originalStudyCount;

    @BeforeEach
    void setUp(){
        String encodedPassword = passwordEncoder.encode("password123");

        member = memberRepository.save(Member.createMember(
                "authorUser",
                encodedPassword,
                "author@test.com",
                "작성자"
        ));

        other = memberRepository.save(Member.createMember(
                "otherUser",
                encodedPassword,
                "other@test.com",
                "다른 회원"
        ));

        study = studyRepository.save(Study.createStudy(
                member,
                "기존 제목",
                "기존 본문",
                StudyMethod.OFFLINE,
                "서울",
                5
        ));

        session = new MockHttpSession();
        session.setAttribute(
                SessionConst.LOGIN_MEMBER,
                LoginMemberSession.from(member)
        );

        originalMemberCount = memberRepository.count();
        originalStudyCount = studyRepository.count();

    }

    @AfterEach
    void cleanUp(){
        // 외래키를 참조하는 자식 데이터부터 정리
        applicationRepository.deleteAllInBatch();
        commentRepository.deleteAllInBatch();
        studyRepository.deleteAllInBatch();
        memberRepository.deleteAllInBatch();
    }

    @ParameterizedTest
    @ValueSource(strings = {"loginId", "email", "nickname"})
    void duplicateFieldsAreRejectedOnJoinAndEdit(String field)
            throws Exception {

        Map<String, String> values = memberValues();

        String duplicateValue = switch (field) {
            case "loginId" -> other.getLoginId();
            case "email" -> other.getEmail();
            default -> other.getNickname();
        };

        values.put(field, duplicateValue);

        // 회원가입에서도 해당 필드에 오류가 표시되어야함
        mockMvc.perform(postForm("/members/join", values))
                .andExpect(status().isOk())
                .andExpect(view().name("members/joinMemberForm"))
                .andExpect(model().attributeHasFieldErrors("member", field));

        assertThat(memberRepository.count()).isEqualTo(originalMemberCount);

        // 회원 수정도 동일하게 해당 필드에 오류 표시
        mockMvc.perform(asMember("/members/edit", values))
                .andExpect(status().isOk())
                .andExpect(view().name("members/editMemberForm"))
                .andExpect(model().attributeHasFieldErrors("member", field));

        // 예외 발생 여부뿐 아니라 DB와 세션도 확인
        assertOriginalMemberAndSession();
    }

    @Test
    void unchangedOwnFieldsAreAccepted() throws Exception {
        Map<String, String> values = memberValues();
        values.put("loginId", member.getLoginId());
        values.put("email", member.getEmail());
        values.put("nickname", member.getNickname());

        mockMvc.perform(asMember("/members/edit", values))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/members/login?updated"));

        Member updated = memberRepository.findById(member.getId())
                .orElseThrow();

        assertThat(session.isInvalid()).isTrue();
        assertThat(updated.getId()).isEqualTo(member.getId());
        assertThat(passwordEncoder.matches(
                values.get("password"),
                updated.getPassword()
        )).isTrue();
    }


    @ParameterizedTest
    @CsvSource({
            "100, 8, true",
            "100, 20, true",
            "101, 20, false",
            "100, 7, false",
            "100, 21, false"
    })
    void memberLengthBoundaries(
            int emailLength,
            int passwordLength,
            boolean accepted) throws Exception {

        Map<String, String> joinValues = memberValues();
        joinValues.put("loginId", "joinedUser");
        joinValues.put("nickname", "새회원");
        joinValues.put("email", emailOfLength(emailLength, "a"));
        joinValues.put("password", "p".repeat(passwordLength));

        Map<String, String> editValues = memberValues();
        editValues.put("email", emailOfLength(emailLength, "c"));
        editValues.put("password", "p".repeat(passwordLength));

        if (accepted) {
            mockMvc.perform(postForm("/members/join", joinValues))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/members/login"));

            mockMvc.perform(asMember("/members/edit", editValues))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/members/login?updated"));

            assertThat(memberRepository.count())
                    .isEqualTo(originalMemberCount + 1);

            Member updated = memberRepository.findById(member.getId())
                    .orElseThrow();

            assertThat(updated.getEmail()).isEqualTo(editValues.get("email"));
            assertThat(passwordEncoder.matches(
                    editValues.get("password"),
                    updated.getPassword()
            )).isTrue();

            // 성공한 수정은 세션을 종료하므로 무효화 여부를 확인
            assertThat(session.isInvalid()).isTrue();

        } else {
            String field = emailLength > 100 ? "email" : "password";

            mockMvc.perform(postForm("/members/join", joinValues))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("member", field));

            mockMvc.perform(asMember("/members/edit", editValues))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("member", field));

            assertThat(memberRepository.count())
                    .isEqualTo(originalMemberCount);

            assertOriginalMemberAndSession();
        }
    }


    @ParameterizedTest
    @ValueSource(strings = {"/members/join", "/members/edit"})
    void databaseUniqueConflictIsShownAsFormError(String path) throws Exception {

        Map<String, String> values = memberValues();
        values.put("email", other.getEmail());

        // 사전 검사에서는 중복이 없다고 나왔지만 실제 DB에는 이미 같은 이메일이 있는 상황을 재현
        doReturn(false)
                .when(memberRepository)
                .existsByEmail(other.getEmail());

        MockHttpServletRequestBuilder request =
                path.equals("/members/edit")
                        ? asMember(path, values)
                        : postForm(path, values);

        String expectedView = path.equals("/members/edit")
                ? "members/editMemberForm"
                : "members/joinMemberForm";

        MvcResult result = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(view().name(expectedView))
                .andExpect(model().attributeHasErrors("member"))
                .andReturn();

        BindingResult errors = (BindingResult) result
                .getModelAndView()
                .getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + "member");

        assertThat(errors.hasGlobalErrors()).isTrue();
        assertThat(errors.getGlobalError().getCode())
                .isEqualTo("duplicate.concurrent");

        assertThat(memberRepository.count()).isEqualTo(originalMemberCount);

        // DB에서 실패한 수정이 롤백됐고 세션도 바뀌지 않았는지 확인
        assertOriginalMemberAndSession();
    }

    @ParameterizedTest
    @MethodSource("invalidStudyInputs")
    void invalidStudyInputDoesNotSaveOrChangeData(String field, String value) throws Exception {

        Map<String, String> values = studyValues();
        values.put(field, value);

        mockMvc.perform(asMember("/studies/new", values))
                .andExpect(status().isOk())
                .andExpect(view().name("studies/registerStudyForm"))
                .andExpect(model().attributeHasFieldErrors("studyForm", field));

        assertThat(studyRepository.count()).isEqualTo(originalStudyCount);

        mockMvc.perform(asMember("/studies/" + study.getId() + "/edit", values))
                .andExpect(status().isOk())
                .andExpect(view().name("studies/editStudyForm"))
                .andExpect(model().attributeHasFieldErrors("editForm", field));

        assertOriginalStudy();
    }

    static Stream<Arguments> invalidStudyInputs() {
        return Stream.of(
                Arguments.of("title", ""),
                Arguments.of("title", "   "),
                Arguments.of("title", "가".repeat(256)),
                Arguments.of("content", "   "),
                Arguments.of("content", "가".repeat(10001)),
                Arguments.of("region", ""),
                Arguments.of("region", "가".repeat(51)),
                Arguments.of("method", ""),
                Arguments.of("capacity", "0")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"ONLINE", "OFFLINE"})
    void studyMaximumLengthsAndRegionPolicyAreAccepted(String method) throws Exception {
        Map<String, String> values = studyValues();
        values.put("title", "가".repeat(255));
        values.put("content", "가".repeat(10000));
        values.put("method", method);
        values.put("region", method.equals("ONLINE") ? "" : "가".repeat(50));

        mockMvc.perform(asMember("/studies/new", values))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/studies"));

        assertThat(studyRepository.count())
                .isEqualTo(originalStudyCount + 1);

        mockMvc.perform(asMember("/studies/" + study.getId() + "/edit", values))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/studies/" + study.getId()));

        Study updated = studyRepository.findById(study.getId())
                .orElseThrow();

        assertThat(updated.getTitle()).hasSize(255);
        assertThat(updated.getContent()).hasSize(10000);
        assertThat(updated.getMethod()).isEqualTo(StudyMethod.valueOf(method));

        if (method.equals("ONLINE")) {
            assertThat(updated.getRegion()).isNull();
        } else {
            assertThat(updated.getRegion()).hasSize(50);
        }
    }





    @ParameterizedTest
    @MethodSource("applicationMessages")
    void applicationMessageBoundaries(String message, boolean accepted) throws Exception {
        long beforeCount = applicationRepository.count();

        MockHttpServletRequestBuilder request = post("/studies/" + study.getId() + "/apply")
                .with(user(LoginMemberPrincipal.from(other)))
                .with(csrf());

        if(message != null){
            request.param("message", message);
        }

        var result = mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/studies/" + study.getId()));

        if(accepted){
            result.andExpect(flash().attributeExists("successMessage"));

            assertThat(applicationRepository.count()).isEqualTo(beforeCount + 1);
        }else{
            result.andExpect(flash().attributeExists("errorMessage"));

            assertThat(applicationRepository.count()).isEqualTo(beforeCount);


            // HTTP 검증을 우회한 직접 생성도 거부되는지 확인
            assertThatThrownBy(() -> Application.createApplication(other, study, message))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    static Stream<Arguments> applicationMessages(){
        return Stream.of(
                Arguments.of(null, false),
                Arguments.of("", false),
                Arguments.of("   ", false),
                Arguments.of("가", true),
                Arguments.of("가".repeat(255), true),
                Arguments.of("가".repeat(256), false)
        );
    }

    @Test
    void domainRejectsInvalidStudyEditBeforeChangingFields(){
        // HTTP 폼 검증 없이 도메인 직접 호출
        assertThatThrownBy(() -> study.changeStudyInfo(
                "바뀌면 안되는 제목",
                "바뀌면 안되는 본문",
                StudyMethod.OFFLINE,
                "가".repeat(51),
                10,
                0L
        )).isInstanceOf(BusinessRuleException.class);

        assertThat(study.getTitle()).isEqualTo("기존 제목");
        assertThat(study.getContent()).isEqualTo("기존 본문");
        assertThat(study.getRegion()).isEqualTo("서울");
        assertThat(study.getCapacity()).isEqualTo(5);
    }

    private Map<String, String> memberValues() {
        return new HashMap<>(Map.of(
                "loginId", "editedUser",
                "password", "newPassword123",
                "email", "edited@test.com",
                "nickname", "수정회원"
        ));
    }

    private Map<String, String> studyValues(){
        return new HashMap<>(Map.of(
                "title", "변경 제목",
                "content", "변경 본문",
                "method", "OFFLINE",
                "region", "부산",
                "capacity", "10"
        ));
    }

    private String emailOfLength(int length, String character){
        // local-part는 64자로 유지해 @Email의 형식 검증 통과 유도
        // 64 + 1(@) + 도메인 길이 + 4(.com) = 전체 길이
        return character.repeat(64)
                + "@"
                + "b".repeat(length - 69)
                + ".com";
    }

    private MockHttpServletRequestBuilder postForm(String path, Map<String, String> values){
        MockHttpServletRequestBuilder request = post(path).with(csrf());

        values.forEach((name, value) -> request.param(name, value));

        return request;
    }

    private MockHttpServletRequestBuilder asMember(String path, Map<String, String> values){
        return postForm(path, values)
                .with(user(LoginMemberPrincipal.from(member)))
                .session(session);
    }

    private void assertOriginalMemberAndSession(){
        // 테스트 전체를 감싼 트랜잭션이 없으므로 DB에서 다시 조회
        Member actual = memberRepository.findById(member.getId()).orElseThrow();

        assertThat(actual.getLoginId()).isEqualTo(member.getLoginId());
        assertThat(actual.getEmail()).isEqualTo(member.getEmail());
        assertThat(actual.getNickname()).isEqualTo(member.getNickname());
        assertThat(actual.getPassword()).isEqualTo(member.getPassword());

        assertThat(session.getAttribute(SessionConst.LOGIN_MEMBER)).isEqualTo(LoginMemberSession.from(member));

    }


    private void assertOriginalStudy(){
        Study actual = studyRepository.findById(study.getId()).orElseThrow();

        assertThat(actual.getTitle()).isEqualTo("기존 제목");
        assertThat(actual.getContent()).isEqualTo("기존 본문");
        assertThat(actual.getMethod()).isEqualTo(StudyMethod.OFFLINE);
        assertThat(actual.getRegion()).isEqualTo("서울");
        assertThat(actual.getCapacity()).isEqualTo(5);
    }
}
