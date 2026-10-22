package com.study.study_community_platform.integration;

import com.study.study_community_platform.domain.Application;
import com.study.study_community_platform.domain.ApplicationStatus;
import com.study.study_community_platform.domain.Comment;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.service.ApplicationService;
import com.study.study_community_platform.service.CommentService;
import com.study.study_community_platform.service.StudyService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceContext;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("mysql")
@ActiveProfiles("mysql-test")
@SpringBootTest(properties = {
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.jpa.properties.hibernate.cache.use_second_level_cache=false",
        "spring.jpa.properties.hibernate.cache.use_query_cache=false",
        "logging.level.org.hibernate.SQL=DEBUG"
})
// 조회와 화면 필드 접근을 같은 트랜잭션에서 측정
// 준비 데이터는 각 테스트가 끝나면 롤백
@Transactional
class MySqlQueryOptimizationTest {

    @PersistenceContext EntityManager entityManager;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired StudyService studyService;
    @Autowired CommentService commentService;
    @Autowired ApplicationService applicationService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    private Statistics statistics;
    private int memberSequence;

    @BeforeEach
    void setUp() throws SQLException {
        // 데이터 정리 전에 전용 MySQL 테스트 DB인지 확인
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName())
                    .isEqualTo("MySQL");

            assertThat(connection.getCatalog())
                    .isEqualTo("study_platform_test");
        }

        // 이 삭제도 테스트 트랜잭션에 포함되며 종료 시 롤백
        jdbc.update("delete from application");
        jdbc.update("delete from comment");
        jdbc.update("delete from study");
        jdbc.update("delete from member");

        statistics = entityManagerFactory
                .unwrap(SessionFactory.class)
                .getStatistics();

        assertThat(statistics.isStatisticsEnabled()).isTrue();
        memberSequence = 0;
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void studyListLoadsAuthorsInOneQuery(int size) {
        List<StudyRow> expected = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            // 작성자가 같으면 1차 캐시 때문에 문제가 작게 보일 수 있음
            Member author = member();
            Study study = study(author, "스터디 " + i);

            if (i == 0) {
                // 탈퇴한 작성자의 기존 글도 조회되어야함
                author.withdraw();
            }

            expected.add(new StudyRow(
                    study.getId(),
                    study.getTitle(),
                    author.getNickname()
            ));
        }

        Study deleted = study(member(), "삭제된 스터디");
        deleted.withdraw();

        var measured = measure(
                "study-list",
                size,
                () -> studyService.findStudies().stream()
                        .map(s -> new StudyRow(
                                s.getId(),
                                s.getTitle(),
                                s.getMember().getNickname()
                        ))
                        .toList()
        );

        assertThat(measured.value())
                .containsExactlyInAnyOrderElementsOf(expected);

        assertOneStatement(measured);
    }

    @Test
    void studyDetailLoadsAuthorInOneQuery() {
        Member author = member();
        Study target = study(author, "상세 화면");
        Long studyId = target.getId();

        StudyRow expected = new StudyRow(
                studyId,
                target.getTitle(),
                author.getNickname()
        );

        var measured = measure("study-detail", 1, () -> {
            Study found = studyService.findStudy(studyId);

            return new StudyRow(
                    found.getId(),
                    found.getTitle(),
                    found.getMember().getNickname()
            );
        });

        assertThat(measured.value()).isEqualTo(expected);
        assertOneStatement(measured);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void commentsLoadAuthorsInOneQuery(int size) {
        Study target = study(member(), "댓글 대상");
        Long studyId = target.getId();
        List<CommentRow> expected = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Member author = member();

            Comment comment = Comment.createComment(
                    author,
                    target,
                    "댓글 " + i
            );
            entityManager.persist(comment);

            if (i == 0) {
                author.withdraw();
            }

            expected.add(new CommentRow(
                    comment.getId(),
                    comment.getContent(),
                    author.getNickname()
            ));
        }

        Comment deleted = Comment.createComment(
                member(),
                target,
                "삭제된 댓글"
        );
        entityManager.persist(deleted);
        deleted.withdraw();

        var measured = measure(
                "comments",
                size,
                () -> commentService.findCommentsByStudyId(studyId).stream()
                        .map(c -> new CommentRow(
                                c.getId(),
                                c.getContent(),
                                c.getMember().getNickname()
                        ))
                        .toList()
        );

        assertThat(measured.value())
                .containsExactlyInAnyOrderElementsOf(expected);

        assertOneStatement(measured);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void applicantsLoadMembersInOneQuery(int size) {
        Study target = study(member(), "신청자 목록");
        Long studyId = target.getId();
        List<ApplicantRow> expected = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Member applicant = member();
            Application application = application(applicant, target);

            if (i == 0) {
                applicant.withdraw();
            }

            expected.add(new ApplicantRow(
                    application.getId(),
                    applicant.getNickname(),
                    application.getStatus()
            ));
        }

        var measured = measure(
                "applicants",
                size,
                () -> applicationService.findApplicationsByStudy(studyId)
                        .stream()
                        .map(a -> new ApplicantRow(
                                a.getId(),
                                a.getMember().getNickname(),
                                a.getStatus()
                        ))
                        .toList()
        );

        assertThat(measured.value())
                .containsExactlyInAnyOrderElementsOf(expected);

        assertOneStatement(measured);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5})
    void myApplicationsLoadStudyOwnersInOneQuery(int size) {
        Member applicant = member();
        Long memberId = applicant.getId();
        List<MyApplicationRow> expected = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Member owner = member();
            Study target = study(owner, "참가 스터디 " + i);
            Application application = application(applicant, target);

            if (i == 0) {
                owner.withdraw();
            }

            expected.add(new MyApplicationRow(
                    application.getId(),
                    target.getTitle(),
                    owner.getNickname(),
                    application.getStatus()
            ));
        }

        Study deleted = study(member(), "삭제된 참가 스터디");
        application(applicant, deleted);
        deleted.withdraw();

        var measured = measure(
                "my-applications",
                size,
                () -> applicationService.findApplicationsByMember(memberId)
                        .stream()
                        .map(a -> new MyApplicationRow(
                                a.getId(),
                                a.getStudy().getTitle(),
                                a.getStudy().getMember().getNickname(),
                                a.getStatus()
                        ))
                        .toList()
        );

        assertThat(measured.value())
                .containsExactlyInAnyOrderElementsOf(expected);

        assertOneStatement(measured);
    }

    @ParameterizedTest
    @EnumSource(ApplicationStatus.class)
    void activeApplicationCheckDoesNotLoadHistory(ApplicationStatus status) {
        Study target = study(member(), "중복 신청 검사");
        Member applicant = member();

        Long memberId = applicant.getId();
        Long studyId = target.getId();

        // 과거 취소 이력을 여러 개 만들어 전체 이력 로딩 감지
        for (int i = 0; i < 20; i++) {
            application(applicant, target).cancel();
        }

        Application latest = application(applicant, target);

        switch (status) {
            case APPROVED -> latest.approve();
            case REJECTED -> latest.reject();
            case CANCELED -> latest.cancel();
            case PENDING -> { }
        }

        var measured = measure(
                "is-applied-" + status,
                21,
                () -> applicationService.isApplied(memberId, studyId)
        );

        // 측정이 끝난 뒤 같은 데이터로 인덱스 검토용 실행 계획 출력
        if (status == ApplicationStatus.PENDING) {
            printApplicationQueryPlans(memberId, studyId);
        }

        assertThat(measured.value()).isEqualTo(
                status == ApplicationStatus.PENDING
                        || status == ApplicationStatus.APPROVED
        );

        assertOneStatement(measured);

        assertThat(measured.entityLoads())
                .as("존재 확인을 위해 신청 이력 엔티티를 로딩하지 않아야 함")
                .isZero();
    }

    @Test
    void noApplicationReturnsFalse() {
        Study target = study(member(), "신청 없는 스터디");
        Member applicant = member();

        Long memberId = applicant.getId();
        Long studyId = target.getId();

        var measured = measure(
                "is-applied-empty",
                0,
                () -> applicationService.isApplied(memberId, studyId)
        );

        assertThat(measured.value()).isFalse();
        assertOneStatement(measured);
        assertThat(measured.entityLoads()).isZero();
    }

    @Test
    void applicationsForOtherMembersAndStudiesDoNotMatch() {
        Study target = study(member(), "검사 대상");
        Study otherStudy = study(member(), "다른 스터디");
        Member applicant = member();

        // 다른 회원의 신청과 다른 스터디의 신청은 검사 결과에 영향을 주면 안됨
        application(member(), target);
        application(applicant, otherStudy);

        Long memberId = applicant.getId();
        Long studyId = target.getId();

        var measured = measure(
                "is-applied-other-resources",
                2,
                () -> applicationService.isApplied(memberId, studyId)
        );

        assertThat(measured.value()).isFalse();
        assertOneStatement(measured);
        assertThat(measured.entityLoads()).isZero();
    }

    private <T> Measurement<T> measure(
            String name,
            int size,
            Supplier<T> action
    ) {
        // 준비 데이터의 INSERT/UPDATE와 1차 캐시를 측정에서 제외
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        System.out.printf(
                "QUERY_START name=%s size=%d%n",
                name,
                size
        );

        // 서비스 호출뿐 아니라 화면에서 사용하는 연관 필드 접근까지 포함
        T value = action.get();

        long statements = statistics.getPrepareStatementCount();
        long entityLoads = statistics.getEntityLoadCount();

        System.out.printf(
                "QUERY_RESULT name=%s size=%d statements=%d entityLoads=%d%n",
                name,
                size,
                statements,
                entityLoads
        );

        return new Measurement<>(value, statements, entityLoads);
    }

    private void assertOneStatement(Measurement<?> measured) {
        assertThat(measured.statements())
                .as("화면 데이터 조회에 필요한 SQL 수")
                .isEqualTo(1L);
    }

    private void printApplicationQueryPlans(Long memberId, Long studyId) {
        // 실제 조회 조건에 대응하는 SQL로 인덱스 후보와 선택 결과 확인
        System.out.println(
                "EXPLAIN_APPROVED_COUNT " + jdbc.queryForList("""
                        explain select count(*)
                        from application
                        where study_id = ?
                          and application_status = 'APPROVED'
                        """, studyId)
        );

        System.out.println(
                "EXPLAIN_ACTIVE_EXISTS " + jdbc.queryForList("""
                        explain select application_id
                        from application
                        where member_id = ?
                          and study_id = ?
                          and application_status in ('PENDING', 'APPROVED')
                        limit 1
                        """, memberId, studyId)
        );
    }

    private Member member() {
        String loginId = "query" + (++memberSequence);

        Member member = Member.createMember(
                loginId,
                passwordEncoder.encode("password123"),
                loginId + "@test.com",
                loginId
        );

        entityManager.persist(member);
        return member;
    }

    private Study study(Member owner, String title) {
        Study study = Study.createStudy(
                owner,
                title,
                "조회 성능 테스트",
                StudyMethod.ONLINE,
                null,
                20
        );

        entityManager.persist(study);
        return study;
    }

    private Application application(Member member, Study study) {
        Application application = Application.createApplication(
                member,
                study,
                "참여 신청"
        );

        entityManager.persist(application);
        return application;
    }

    private record Measurement<T>(
            T value,
            long statements,
            long entityLoads
    ) {}

    private record StudyRow(
            Long id,
            String title,
            String nickname
    ) {}

    private record CommentRow(
            Long id,
            String content,
            String nickname
    ) {}

    private record ApplicantRow(
            Long id,
            String nickname,
            ApplicationStatus status
    ) {}

    private record MyApplicationRow(
            Long id,
            String title,
            String owner,
            ApplicationStatus status
    ) {}
}