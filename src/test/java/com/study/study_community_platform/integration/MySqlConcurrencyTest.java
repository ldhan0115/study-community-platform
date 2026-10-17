package com.study.study_community_platform.integration;

import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.domain.ApplicationStatus;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.exception.BusinessRuleException;
import com.study.study_community_platform.service.ApplicationService;
import com.study.study_community_platform.service.MemberService;
import com.study.study_community_platform.service.StudyService;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@Tag("mysql")
@Tag("mysql-concurrency")
@ActiveProfiles("mysql-test")
@SpringBootTest(properties = {
        "spring.datasource.hikari.maximum-pool-size=4",
        "spring.datasource.hikari.connection-timeout=10000",

        // 잠금 실패 검증의 대기 시간을 줄이기 위한 테스트 전용 설정
        "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout = 2"
})
// 클래스와 테스트 메서드에 @Transactional을 붙이지 않음
class MySqlConcurrencyTest {

    @Autowired MemberService memberService;
    @Autowired StudyService studyService;
    @Autowired ApplicationService applicationService;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;

    private ExecutorService executor;
    private Long ownerId;
    private Long firstMemberId;
    private Long secondMemberId;

    @BeforeEach
    void setUp() throws SQLException {
        assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                .as("테스트 전체를 하나의 트랜잭션으로 감싸면 안 됨")
                .isFalse();

        cleanTestData();
        executor = Executors.newFixedThreadPool(2);

        // 각 서비스 호출이 반환되면서 준비 데이터가 실제로 커밋
        ownerId = join("raceOwner");
        firstMemberId = join("raceFirst");
        secondMemberId = join("raceSecond");

        System.out.printf("MySQL=%s, isolation=%s%n",
                jdbc.queryForObject("select version()", String.class),
                jdbc.queryForObject(
                        "select @@session.transaction_isolation",
                        String.class
                ));
    }

    @AfterEach
    void tearDown() throws SQLException, InterruptedException {
        if (executor != null) {
            executor.shutdownNow();

            // 실행 중인 작업이 남아 있으면 DB 정리를 진행하지 않음
            assertThat(executor.awaitTermination(15, SECONDS))
                    .as("작업 종료 실패: 실행 중인 트랜잭션을 먼저 확인")
                    .isTrue();
        }

        cleanTestData();
    }

    @Test
    void concurrentApprovalsMustNotExceedCapacity()
            throws InterruptedException {

        Long studyId = createStudy(1);

        Long firstId = applicationService.applyToStudy(
                firstMemberId, studyId, "첫 번째 신청"
        );

        Long secondId = applicationService.applyToStudy(
                secondMemberId, studyId, "두 번째 신청"
        );

        List<Outcome> outcomes = race(
                new Operation(
                        "approve-first",
                        () -> applicationService.approveApplication(
                                ownerId, firstId
                        )
                ),
                new Operation(
                        "approve-second",
                        () -> applicationService.approveApplication(
                                ownerId, secondId
                        )
                )
        );

        // 모든 작업의 커밋/롤백이 끝난 뒤 JPA 1차 캐시를 거치지 않고 DB를 다시 조회
        Long approved = approvedCount(studyId);

        String studyStatus = jdbc.queryForObject(
                "select study_status from study where study_id = ?",
                String.class,
                studyId
        );

        System.out.printf(
                "capacity=1, approved=%d, studyStatus=%s%n",
                approved,
                studyStatus
        );

        SoftAssertions softly = new SoftAssertions();

        assertOneSuccessfulCommit(softly, outcomes);

        softly.assertThat(approved)
                .as("정원 1에서 최종 승인 수")
                .isEqualTo(1L);

        softly.assertThat(studyStatus)
                .isEqualTo("CLOSED");

        softly.assertAll();
    }

    @Test
    void concurrentApplicationsMustNotCreateDuplicates()
            throws InterruptedException {

        Long studyId = createStudy(2);

        List<Outcome> outcomes = race(
                new Operation(
                        "apply-first",
                        () -> applicationService.applyToStudy(
                                firstMemberId, studyId, "첫 요청"
                        )
                ),
                new Operation(
                        "apply-second",
                        () -> applicationService.applyToStudy(
                                firstMemberId, studyId, "두 번째 요청"
                        )
                )
        );

        Long activeCount = jdbc.queryForObject("""
                select count(*) from application
                where member_id = ? and study_id = ?
                  and application_status in ('PENDING', 'APPROVED')
                """,
                Long.class,
                firstMemberId,
                studyId
        );

        System.out.printf(
                "activeApplications=%d%n",
                activeCount
        );

        SoftAssertions softly = new SoftAssertions();

        assertOneSuccessfulCommit(softly, outcomes);

        softly.assertThat(activeCount)
                .as("같은 회원·스터디의 유효 신청 수")
                .isEqualTo(1L);

        softly.assertAll();
    }

    @ParameterizedTest
    @EnumSource(
            value = ApplicationStatus.class,
            names = {"REJECTED", "CANCELED"}
    )
    void onlyOneTerminalTransitionMustSucceed(
            ApplicationStatus competingStatus
    ) throws InterruptedException {

        Long studyId = createStudy(2);

        Long applicationId = applicationService.applyToStudy(
                firstMemberId, studyId, "참여 신청"
        );

        Runnable competingAction =
                competingStatus == ApplicationStatus.REJECTED
                        ? () -> applicationService.rejectApplication(
                        ownerId, applicationId
                )
                        : () -> applicationService.cancelApplication(
                        firstMemberId, applicationId
                );

        List<Outcome> outcomes = race(
                new Operation(
                        "APPROVED",
                        () -> applicationService.approveApplication(
                                ownerId, applicationId
                        )
                ),
                new Operation(
                        competingStatus.name(),
                        competingAction
                )
        );

        String finalStatus = jdbc.queryForObject(
                """
                select application_status
                from application
                where application_id = ?
                """,
                String.class,
                applicationId
        );

        System.out.printf(
                "competingStatus=%s, finalStatus=%s, approved=%d%n",
                competingStatus,
                finalStatus,
                approvedCount(studyId)
        );

        SoftAssertions softly = new SoftAssertions();

        // DB에 상태가 하나만 저장된다는 사실만으로는 두 요청이 모두 성공한 문제를 발견할 수 없음
        assertOneSuccessfulCommit(softly, outcomes);

        softly.assertThat(finalStatus)
                .isIn("APPROVED", competingStatus.name());

        List<Outcome> successful = outcomes.stream()
                .filter(Outcome::succeeded)
                .toList();

        if (successful.size() == 1) {
            softly.assertThat(finalStatus)
                    .as("성공한 작업과 최종 상태 일치")
                    .isEqualTo(successful.get(0).operation());
        }

        softly.assertAll();
    }

    @Test
    void capacityReductionAndApprovalKeepCapacityConsistent()
            throws InterruptedException {

        Long studyId = createStudy(2);

        Long firstId = applicationService.applyToStudy(
                firstMemberId, studyId, "첫 신청"
        );

        Long secondId = applicationService.applyToStudy(
                secondMemberId, studyId, "두 번째 신청"
        );

        applicationService.approveApplication(ownerId, firstId);

        // 승인 1명, 정원 2명 상태에서 승인과 정원 축소를 동시에 요청
        List<Outcome> outcomes = race(
                new Operation(
                        "approve",
                        () -> applicationService.approveApplication(
                                ownerId, secondId
                        )
                ),
                new Operation(
                        "shrink",
                        () -> studyService.updateStudy(
                                ownerId,
                                studyId,
                                "정원 변경",
                                "정원 변경 검증",
                                StudyMethod.ONLINE,
                                null,
                                1
                        )
                )
        );

        Integer capacity = jdbc.queryForObject(
                "select capacity from study where study_id = ?",
                Integer.class,
                studyId
        );

        Long approved = approvedCount(studyId);
        String secondStatus = applicationStatus(secondId);

        System.out.printf(
                "capacity=%d, approved=%d, secondStatus=%s%n",
                capacity,
                approved,
                secondStatus
        );

        SoftAssertions softly = new SoftAssertions();

        assertOneSuccessfulCommit(softly, outcomes);

        // 승인 우선이면 정원 2 / 승인 2
        // 축소 우선이면 정원 1 / 승인 1
        softly.assertThat(approved)
                .isEqualTo(capacity.longValue());

        softly.assertThat(approved)
                .isIn(1L, 2L);

        softly.assertThat(jdbc.queryForObject(
                "select study_status from study where study_id = ?",
                String.class,
                studyId
        )).isEqualTo("CLOSED");

        List<Outcome> successful = outcomes.stream()
                .filter(Outcome::succeeded)
                .toList();

        if (successful.size() == 1) {
            boolean approvalWon =
                    successful.get(0).operation().equals("approve");

            softly.assertThat(approved)
                    .isEqualTo(approvalWon ? 2L : 1L);

            softly.assertThat(secondStatus)
                    .isEqualTo(approvalWon ? "APPROVED" : "PENDING");
        }

        softly.assertAll();
    }

    @ParameterizedTest
    @EnumSource(
            value = ApplicationStatus.class,
            names = {"REJECTED", "CANCELED"}
    )
    void reapplicationAfterFinalStateStillWorks(
            ApplicationStatus previousStatus
    ) {
        Long studyId = createStudy(2);

        Long oldId = applicationService.applyToStudy(
                firstMemberId, studyId, "기존 신청"
        );

        if (previousStatus == ApplicationStatus.REJECTED) {
            applicationService.rejectApplication(ownerId, oldId);
        } else {
            applicationService.cancelApplication(firstMemberId, oldId);
        }

        Long newId = applicationService.applyToStudy(
                firstMemberId, studyId, "재신청"
        );

        // 이전 신청 이력은 보존하고 새로운 PENDING 신청 생성
        assertThat(newId).isNotEqualTo(oldId);

        assertThat(applicationStatus(oldId))
                .isEqualTo(previousStatus.name());

        assertThat(applicationStatus(newId))
                .isEqualTo("PENDING");

        assertThat(jdbc.queryForObject("""
            select count(*) from application
            where member_id = ? and study_id = ?
              and application_status in ('PENDING', 'APPROVED')
            """,
                Long.class,
                firstMemberId,
                studyId
        )).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "apply", "approve", "reject", "cancel", "update", "delete"
    })
    void lockedStudyRejectsMutationWithoutChangingData(
            String operation
    ) throws Exception {

        Long studyId = createStudy(2);

        Long applicationId = applicationService.applyToStudy(
                firstMemberId, studyId, "기존 신청"
        );

        var studyBefore = jdbc.queryForMap(
                "select * from study where study_id = ?",
                studyId
        );

        var applicationsBefore = jdbc.queryForList(
                """
                select * from application
                where study_id = ?
                order by application_id
                """,
                studyId
        );

        Runnable action = switch (operation) {
            case "apply" -> () -> applicationService.applyToStudy(
                    secondMemberId, studyId, "새 신청"
            );

            case "approve" -> () -> applicationService.approveApplication(
                    ownerId, applicationId
            );

            case "reject" -> () -> applicationService.rejectApplication(
                    ownerId, applicationId
            );

            case "cancel" -> () -> applicationService.cancelApplication(
                    firstMemberId, applicationId
            );

            case "update" -> () -> studyService.updateStudy(
                    ownerId,
                    studyId,
                    "바뀌면 안 되는 제목",
                    "바뀌면 안 되는 본문",
                    StudyMethod.ONLINE,
                    null,
                    3
            );

            case "delete" -> () -> studyService.deleteStudy(
                    ownerId, studyId
            );

            default -> throw new IllegalArgumentException(operation);
        };

        // 다른 트랜잭션이 실제로 잠금을 획득한 뒤 요청
        // 잠금 시간 초과가 발생해야 하며 데이터는 보존되어야함
        withStudyLock(
                studyId,
                () -> assertThatThrownBy(action::run)
                        .isInstanceOf(PessimisticLockingFailureException.class)
        );

        assertThat(jdbc.queryForMap(
                "select * from study where study_id = ?",
                studyId
        )).isEqualTo(studyBefore);

        assertThat(jdbc.queryForList(
                """
                select * from application
                where study_id = ?
                order by application_id
                """,
                studyId
        )).isEqualTo(applicationsBefore);
    }

    @Test
    void differentStudyCanChangeWhileOneStudyIsLocked() throws Exception {
        Long lockedStudyId = createStudy(2);
        Long otherStudyId = createStudy(2);

        // 첫 번째 스터디의 잠금이 유지되는 동안
        // 다른 스터디에는 정상적으로 신청할 수 있어야함
        withStudyLock(lockedStudyId, () -> {
            Long applicationId = applicationService.applyToStudy(
                    firstMemberId,
                    otherStudyId,
                    "다른 스터디 신청"
            );

            assertThat(applicationStatus(applicationId))
                    .isEqualTo("PENDING");
        });
    }

    private String applicationStatus(Long applicationId) {
        return jdbc.queryForObject(
                """
                select application_status
                from application
                where application_id = ?
                """,
                String.class,
                applicationId
        );
    }

    private void withStudyLock(
            Long studyId,
            Runnable action
    ) throws Exception {

        CountDownLatch locked = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Future<?> holder = executor.submit(() -> {
            // 이 트랜잭션은 잠금을 보유하는 테스트용 작업
            // 검증 대상 서비스 호출을 감싸는 용도 x
            TransactionTemplate transaction =
                    new TransactionTemplate(transactionManager);

            transaction.setPropagationBehavior(
                    TransactionDefinition.PROPAGATION_REQUIRES_NEW
            );

            transaction.executeWithoutResult(status -> {
                jdbc.queryForObject(
                        """
                        select study_id
                        from study
                        where study_id = ?
                        for update
                        """,
                        Long.class,
                        studyId
                );

                // 실제 잠금을 얻은 뒤 검증 작업의 시작을 허용
                locked.countDown();

                try {
                    if (!release.await(20, SECONDS)) {
                        throw new IllegalStateException(
                                "잠금 해제 신호 대기 시간 초과"
                        );
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();

                    throw new IllegalStateException(
                            "잠금 보유 작업 중단",
                            e
                    );
                }
            });
        });

        try {
            assertThat(locked.await(10, SECONDS))
                    .as("테스트용 Study 잠금 획득")
                    .isTrue();

            action.run();

        } finally {
            release.countDown();

            // 잠금 보유 작업의 예외와 커밋 완료도 확인
            holder.get(30, SECONDS);
        }
    }

    private List<Outcome> race(
            Operation first,
            Operation second
    ) throws InterruptedException {

        List<Operation> operations = List.of(first, second);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        List<Future<Void>> futures = new ArrayList<>();

        for (Operation operation : operations) {
            futures.add(executor.submit(() -> {
                ready.countDown();

                if (!start.await(10, SECONDS)) {
                    throw new IllegalStateException(
                            "작업 시작 대기 시간 초과"
                    );
                }

                // 바깥 TransactionTemplate 없이 실제 서비스 프록시 호출
                // 서비스의 @Transactional과 READ_COMMITTED 적용
                operation.action().run();

                // 서비스 프록시가 정상 반환하면 커밋도 완료
                return null;
            }));
        }

        try {
            assertThat(ready.await(10, SECONDS))
                    .as("두 작업의 시작 준비")
                    .isTrue();
        } finally {
            start.countDown();
        }

        List<Outcome> outcomes = new ArrayList<>();

        for (int i = 0; i < futures.size(); i++) {
            String name = operations.get(i).name();

            try {
                futures.get(i).get(40, SECONDS);

                outcomes.add(new Outcome(name, null));

            } catch (ExecutionException e) {
                // 하나가 실패하더라도 나머지 결과까지 확인
                outcomes.add(new Outcome(name, e.getCause()));

            } catch (TimeoutException e) {
                futures.get(i).cancel(true);

                outcomes.add(new Outcome(name, e));
            }
        }

        for (Outcome outcome : outcomes) {
            System.out.printf(
                    "operation=%s, commitConfirmed=%s, exception=%s%n",
                    outcome.operation(),
                    outcome.succeeded(),
                    outcome.failure() == null
                            ? "none"
                            : outcome.failure().getClass().getName()
            );

            if (outcome.failure() != null) {
                outcome.failure().printStackTrace(System.out);
            }
        }

        executor.shutdown();

        assertThat(executor.awaitTermination(15, SECONDS))
                .as("최종 DB 조회 전에 모든 작업이 종료되어야 함")
                .isTrue();

        assertThat(outcomes)
                .as("Future 타임아웃을 정상적인 요청 거부로 취급하지 않음")
                .noneMatch(
                        outcome ->
                                outcome.failure() instanceof TimeoutException
                );

        return outcomes;
    }

    private static void assertOneSuccessfulCommit(
            SoftAssertions softly,
            List<Outcome> outcomes
    ) {
        softly.assertThat(
                        outcomes.stream()
                                .filter(Outcome::succeeded)
                                .count()
                )
                .as("정상 커밋은 정확히 한 건이어야 함")
                .isEqualTo(1L);

        outcomes.stream()
                .filter(outcome -> !outcome.succeeded())
                .forEach(outcome ->
                        softly.assertThat(outcome.failure())
                                .as(
                                        "%s 실패 원인: 타임아웃·DB 장애를 정상 충돌로 취급하지 않음",
                                        outcome.operation()
                                )
                                .isInstanceOf(BusinessRuleException.class)
                );
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

    private Long createStudy(int capacity) {
        return studyService.registerStudy(
                ownerId,
                "동시성 검증 스터디",
                "동시 요청 재현용 본문",
                StudyMethod.ONLINE,
                null,
                capacity
        );
    }

    private Long approvedCount(Long studyId) {
        return jdbc.queryForObject("""
                select count(*) from application
                where study_id = ?
                  and application_status = 'APPROVED'
                """,
                Long.class,
                studyId
        );
    }

    private void cleanTestData() throws SQLException {
        // 전용 테스트 DB인지 확인한 뒤에만 삭제
        try (Connection connection = dataSource.getConnection()) {
            assertThat(
                    connection.getMetaData().getDatabaseProductName()
            ).isEqualTo("MySQL");

            assertThat(connection.getCatalog())
                    .isEqualTo("study_platform_test");
        }

        // FK의 자식부터 삭제, Flyway 이력은 유지
        jdbc.update("delete from application");
        jdbc.update("delete from comment");
        jdbc.update("delete from study");
        jdbc.update("delete from member");
    }

    private record Operation(String name, Runnable action) {}

    private record Outcome(String operation, Throwable failure) {

        boolean succeeded() {
            return failure == null;
        }
    }
}