package com.study.study_community_platform.domain;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {

    @Test
    void newApplicationStartsPending(){
        //when
        Application application = createPendingApplication();

        //then
        assertThat(application.getStatus()).isEqualTo(ApplicationStatus.PENDING);
    }

    @ParameterizedTest(name = "PENDING 상태에서 {0} 상태로 변경할 수 있다.")
    @EnumSource(
            value = ApplicationStatus.class,
            names = {
                    "APPROVED",
                    "REJECTED",
                    "CANCELED"
            }
    )
    void pendingCanTransitionToTerminalStatus(ApplicationStatus nextStatus){
        // given
        Application application = createPendingApplication();

        // when
        transitionTo(application, nextStatus);

        // then
        assertThat(application.getStatus()).isEqualTo(nextStatus);
    }

    @ParameterizedTest(name = "{0} 상태에서 {1} 상태로 변경할 수 없다.")
    @MethodSource("forbiddenTransitions")
    void terminalStatusCannotBeChanged(ApplicationStatus currentStatus,
                                       ApplicationStatus nextStatus){
        // given
        Application application = createPendingApplication();

        transitionTo(application, currentStatus);

        // when & then
        assertThatThrownBy(() ->
                transitionTo(application, nextStatus))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("대기 중인 신청만 상태를 변경할 수 있습니다.");

        assertThat(application.getStatus()).isEqualTo(currentStatus);

    }

    static Stream<Arguments> forbiddenTransitions(){
        List<ApplicationStatus> terminalStatuses = List.of(
                ApplicationStatus.APPROVED,
                ApplicationStatus.REJECTED,
                ApplicationStatus.CANCELED
        );

        return terminalStatuses.stream()
                .flatMap(currentStatus ->
                        terminalStatuses.stream()
                                .map(nextStatus ->
                                        Arguments.of(
                                                currentStatus,
                                                nextStatus
                                        )
                                )
                );
    }

    private void transitionTo(Application application, ApplicationStatus nextStatus){
        switch(nextStatus){
            case APPROVED -> application.approve();
            case REJECTED -> application.reject();
            case CANCELED -> application.cancel();

            case PENDING -> throw new IllegalArgumentException("PENDING 상태로 되돌리는 전이는 지원하지 않습니다.");
        }
    }

    private Application createPendingApplication(){
        Member studyOwner = Member.createMember("owner", "1234",
                "owner@test.com", "오너");

        Member applicant = Member.createMember("applicant", "1234",
                "applicant@test.com", "신청자");

        Study study = Study.createStudy(studyOwner, "스터디",
                "스터디 내용", StudyMethod.ONLINE, null, 5);

        return Application.createApplication(applicant, study, "참여하고 싶습니다.");

    }

}