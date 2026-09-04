package com.study.study_community_platform;

import com.study.study_community_platform.domain.Application;
import com.study.study_community_platform.domain.Comment;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.repository.ApplicationRepository;
import com.study.study_community_platform.repository.CommentRepository;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Profile("local")
public class TestDataInit {

    private final MemberRepository memberRepository;
    private final StudyRepository studyRepository;
    private final CommentRepository commentRepository;
    private final ApplicationRepository applicationRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void init() {

        // 애플리케이션 재실행 시 테스트 데이터가 중복 저장되는 것을 방지
        if (memberRepository.existsByLoginId("test1")) {
            return;
        }

        /*
         * 회원 데이터
         *
         * README 화면 촬영 시 사용할 수 있는 계정
         * test1 / test1234 : 스터디 방장 화면
         * test2 / test1234 : 참가 신청 회원 화면
         */
        Member member1 = Member.createMember(
                "test1",
                passwordEncoder.encode("test1234"),
                "test1@example.com",
                "김개발"
        );

        Member member2 = Member.createMember(
                "test2",
                passwordEncoder.encode("test1234"),
                "test2@example.com",
                "이자바"
        );

        Member member3 = Member.createMember(
                "test3",
                passwordEncoder.encode("test1234"),
                "test3@example.com",
                "박스프링"
        );

        Member member4 = Member.createMember(
                "test4",
                passwordEncoder.encode("test1234"),
                "test4@example.com",
                "최데이터"
        );

        Member member5 = Member.createMember(
                "test5",
                passwordEncoder.encode("test1234"),
                "test5@example.com",
                "정코딩"
        );

        Member member6 = Member.createMember(
                "test6",
                passwordEncoder.encode("test1234"),
                "test6@example.com",
                "한백엔드"
        );

        memberRepository.saveAll(
                java.util.List.of(
                        member1,
                        member2,
                        member3,
                        member4,
                        member5,
                        member6
                )
        );

        /*
         * 스터디 데이터
         *
         * 온라인과 오프라인,
         * 서로 다른 지역과 모집 정원을 섞어서 생성
         */
        Study javaStudy = Study.createStudy(
                member1,
                "Java 기초부터 다시 공부해요",
                """
                Java의 객체지향 개념부터 컬렉션, 예외 처리까지 함께 공부하는 스터디입니다.
                매주 학습 내용을 정리하고 서로 코드 리뷰를 진행합니다.
                """,
                StudyMethod.ONLINE,
                null,
                5
        );

        Study springStudy = Study.createStudy(
                member1,
                "Spring Boot 프로젝트 스터디",
                """
                Spring MVC, JPA, 테스트 코드를 활용해 개인 프로젝트를 개선합니다.
                매주 구현한 기능과 발생한 문제를 공유하고 함께 해결합니다.
                """,
                StudyMethod.OFFLINE,
                "서울",
                4
        );

        Study algorithmStudy = Study.createStudy(
                member2,
                "주말 알고리즘 문제 풀이",
                """
                코딩 테스트 준비를 위해 매주 알고리즘 문제를 풀고 풀이를 공유합니다.
                Java를 사용하며 초급자도 참여할 수 있습니다.
                """,
                StudyMethod.ONLINE,
                null,
                6
        );

        Study jpaStudy = Study.createStudy(
                member3,
                "JPA 성능 최적화 스터디",
                """
                영속성 컨텍스트, 지연 로딩, N+1 문제와 쿼리 최적화를 공부합니다.
                예제 코드를 작성하고 실행 결과를 함께 분석합니다.
                """,
                StudyMethod.OFFLINE,
                "수원",
                4
        );

        Study interviewStudy = Study.createStudy(
                member4,
                "백엔드 기술 면접 준비",
                """
                Java, Spring, 데이터베이스와 네트워크 질문을 정리하고
                매주 모의 면접을 진행합니다.
                """,
                StudyMethod.ONLINE,
                null,
                5
        );

        Study databaseStudy = Study.createStudy(
                member5,
                "데이터베이스 설계와 SQL",
                """
                관계형 데이터베이스 모델링과 인덱스, 조인, 실행 계획을 학습합니다.
                실제 서비스 요구사항을 바탕으로 ERD도 함께 작성합니다.
                """,
                StudyMethod.OFFLINE,
                "성남",
                3
        );

        Study closedStudy = Study.createStudy(
                member6,
                "Spring Security 입문",
                """
                인증과 인가, 세션, CSRF, 비밀번호 암호화를 공부하는 스터디입니다.
                현재 모집이 마감되었습니다.
                """,
                StudyMethod.ONLINE,
                null,
                3
        );

        // README에서 모집 마감 상태를 보여주기 위한 데이터
        closedStudy.close();

        studyRepository.saveAll(
                java.util.List.of(
                        javaStudy,
                        springStudy,
                        algorithmStudy,
                        jpaStudy,
                        interviewStudy,
                        databaseStudy,
                        closedStudy
                )
        );

        /*
         * 댓글 데이터
         */
        Comment comment1 = Comment.createComment(
                member2,
                javaStudy,
                "Java 문법은 알고 있는데 객체지향부터 다시 정리하고 싶습니다."
        );

        Comment comment2 = Comment.createComment(
                member3,
                javaStudy,
                "온라인 진행 시간은 언제인가요?"
        );

        Comment comment3 = Comment.createComment(
                member1,
                javaStudy,
                "매주 수요일 오후 8시에 진행할 예정입니다."
        );

        Comment comment4 = Comment.createComment(
                member4,
                springStudy,
                "개인 프로젝트가 없어도 참여할 수 있을까요?"
        );

        Comment comment5 = Comment.createComment(
                member1,
                springStudy,
                "간단한 CRUD 프로젝트부터 함께 만들어도 괜찮습니다."
        );

        Comment comment6 = Comment.createComment(
                member5,
                jpaStudy,
                "N+1 문제와 fetch join도 다루는지 궁금합니다."
        );

        Comment comment7 = Comment.createComment(
                member6,
                interviewStudy,
                "모의 면접은 몇 명씩 진행하나요?"
        );

        Comment comment8 = Comment.createComment(
                member2,
                databaseStudy,
                "SQL 실행 계획 분석에 관심이 있어 참여하고 싶습니다."
        );

        commentRepository.saveAll(
                java.util.List.of(
                        comment1,
                        comment2,
                        comment3,
                        comment4,
                        comment5,
                        comment6,
                        comment7,
                        comment8
                )
        );

        /*
         * 참가 신청 데이터
         *
         * test1 계정으로 로그인하면
         * 자신이 개설한 스터디의 신청 관리 화면을 촬영할 수 있음
         */

        // Java 스터디 신청: 승인 대기
        Application pendingApplication1 = Application.createApplication(
                member2,
                javaStudy,
                "Java 기본기를 체계적으로 다시 공부하고 싶어 신청합니다."
        );

        // Java 스터디 신청: 승인 대기
        Application pendingApplication2 = Application.createApplication(
                member3,
                javaStudy,
                "매주 빠지지 않고 참여하겠습니다."
        );

        // Java 스터디 신청: 승인 완료
        Application approvedApplication = Application.createApplication(
                member4,
                javaStudy,
                "백엔드 취업 준비를 하며 Java를 복습하고 있습니다."
        );
        approvedApplication.approve();

        // Spring Boot 스터디 신청: 거절
        Application rejectedApplication = Application.createApplication(
                member5,
                springStudy,
                "Spring Boot 프로젝트 경험을 쌓고 싶습니다."
        );
        rejectedApplication.reject();

        // Spring Boot 스터디 신청: 취소
        Application canceledApplication = Application.createApplication(
                member6,
                springStudy,
                "일정이 맞지 않아 신청을 취소했습니다."
        );
        canceledApplication.cancel();

        // 다른 회원이 개설한 스터디에 test1 회원이 신청한 데이터
        Application myApplication = Application.createApplication(
                member1,
                algorithmStudy,
                "코딩 테스트 준비를 위해 참여하고 싶습니다."
        );

        applicationRepository.saveAll(
                java.util.List.of(
                        pendingApplication1,
                        pendingApplication2,
                        approvedApplication,
                        rejectedApplication,
                        canceledApplication,
                        myApplication
                )
        );
    }
}