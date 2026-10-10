package com.study.study_community_platform.integration;

import com.study.study_community_platform.TestDataInit;
import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.service.ApplicationService;
import com.study.study_community_platform.service.CommentService;
import com.study.study_community_platform.service.MemberService;
import com.study.study_community_platform.service.StudyService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

@Tag("mysql")
@ActiveProfiles("mysql-test")
@SpringBootTest

// 테스트 전체에 @Transactional을 붙이지 않음
// 서비스 호출이 끝난 뒤 실제 커밋된 데이터를 다시 조회
class MySqlPersistenceTest {

    @Autowired MemberService memberService;
    @Autowired StudyService studyService;
    @Autowired ApplicationService applicationService;
    @Autowired CommentService commentService;
    @Autowired MemberRepository memberRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired Flyway flyway;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired ApplicationContext context;

    @BeforeEach
    void setUp() throws SQLException {
        cleanTestData();
    }

    @AfterEach
    void tearDown() throws SQLException {
        cleanTestData();
    }

    @Test
    void migrationCanRunAgainWithoutLosingData() {
        Long memberId = join("mysqlOwner");

        // MySQL 테스트 환경에서 샘플 초기화 빈이 등록되지 않아야 함
        assertThat(context.getBeansOfType(TestDataInit.class))
                .isEmpty();

        assertThat(jdbc.queryForObject("""
                select count(*)
                from flyway_schema_history
                where version = '1'
                  and success = true
                """,
                Integer.class
        )).isEqualTo(1);

        // 이미 적용한 마이그레이션을 다시 적용하지 않음
        flyway.validate();

        assertThat(flyway.migrate().migrationsExecuted)
                .isZero();

        // 기존 회원 데이터도 유지
        assertThat(memberRepository.findById(memberId))
                .isPresent();

        assertThat(memberRepository.count())
                .isEqualTo(1);
    }

    @Test
    void coreFlowPersistsKoreanTextAndEnumValues() {
        Long ownerId = join("mysqlOwner");
        Long applicantId = join("mysqlApplicant");

        String studyContent = "가".repeat(10_000);
        String commentContent = "MySQL에서도 한글과 이모지 저장 😊";

        Long studyId = studyService.registerStudy(
                ownerId,
                "MySQL 스터디",
                studyContent,
                StudyMethod.ONLINE,
                null,
                1
        );

        Long commentId = commentService.registerComment(
                applicantId,
                studyId,
                commentContent
        );

        Long applicationId = applicationService.applyToStudy(
                applicantId,
                studyId,
                "참여하겠습니다."
        );

        applicationService.approveApplication(
                ownerId,
                applicationId
        );

        // 서비스 트랜잭션이 끝난 뒤 JDBC로 저장 결과를 확인
        assertThat(jdbc.queryForObject(
                "select study_content from study where study_id = ?",
                String.class,
                studyId
        )).isEqualTo(studyContent);

        assertThat(jdbc.queryForObject(
                "select content from comment where comment_id = ?",
                String.class,
                commentId
        )).isEqualTo(commentContent);

        assertThat(jdbc.queryForObject(
                "select method from study where study_id = ?",
                String.class,
                studyId
        )).isEqualTo("ONLINE");

        assertThat(jdbc.queryForObject(
                "select application_status from application where application_id = ?",
                String.class,
                applicationId
        )).isEqualTo("APPROVED");

        assertThat(jdbc.queryForObject(
                "select study_status from study where study_id = ?",
                String.class,
                studyId
        )).isEqualTo("CLOSED");

        Member saved = memberRepository.findById(ownerId)
                .orElseThrow();

        assertThat(passwordEncoder.matches(
                "password123",
                saved.getPassword()
        )).isTrue();

        // 실제 MySQL에서 대소문자 구분 정책 확인
        assertThat(
                memberRepository.findByLoginIdAndDeletedAtIsNull(
                        "MYSQLOWNER"
                )
        ).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"loginId", "email", "nickname"})
    void databaseUniqueConstraintsRejectDuplicates(String field) {
        Long memberId = join("mysqlOwner");

        Member original = memberRepository.findById(memberId)
                .orElseThrow();

        Member duplicate = Member.createMember(
                field.equals("loginId")
                        ? original.getLoginId()
                        : "otherLogin",

                passwordEncoder.encode("password123"),

                field.equals("email")
                        ? original.getEmail()
                        : "other@test.com",

                field.equals("nickname")
                        ? original.getNickname()
                        : "다른닉네임"
        );

        // 서비스의 사전 검사를 우회하여 DB UNIQUE 제약 자체를 검사
        assertThatThrownBy(
                () -> memberRepository.saveAndFlush(duplicate)
        ).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(memberRepository.count())
                .isEqualTo(1);
    }

    @Test
    void databaseRejectsCommentForMissingStudy() {
        Long memberId = join("mysqlOwner");

        // Hibernate validate만으로 확인되지 않는 FK를 실제 INSERT로 검사
        assertThatThrownBy(() -> jdbc.update("""
                insert into comment (
                    study_id,
                    member_id,
                    content,
                    created_at,
                    updated_at
                )
                values (
                    ?, ?, ?,
                    current_timestamp(6),
                    current_timestamp(6)
                )
                """,
                Long.MAX_VALUE,
                memberId,
                "저장되면 안 되는 댓글"
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.queryForObject(
                "select count(*) from comment",
                Integer.class
        )).isZero();
    }

    private Long join(String loginId) {
        return memberService.join(
                new JoinMemberForm(
                        loginId,
                        "password123",
                        loginId + "@test.com",
                        loginId
                )
        );
    }

    private void cleanTestData() throws SQLException {
        // 연결한 DB가 전용 테스트 DB인지 확인한 뒤에만 데이터 정리
        try (Connection connection = dataSource.getConnection()) {
            assertThat(
                    connection.getMetaData().getDatabaseProductName()
            ).isEqualTo("MySQL");

            assertThat(connection.getCatalog())
                    .isEqualTo("study_platform_test");
        }

        // FK를 참조하는 자식부터 삭제
        jdbc.update("delete from application");
        jdbc.update("delete from comment");
        jdbc.update("delete from study");
        jdbc.update("delete from member");

        // Flyway 적용 이력은 유지
    }
}