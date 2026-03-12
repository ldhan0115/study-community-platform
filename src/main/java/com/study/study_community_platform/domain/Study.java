package com.study.study_community_platform.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Study {

    // 스터디의 기본 키 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "study_id")
    private Long id;

    // 스터디를 개설한 회원
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 스터디 모집글 제목
    @Column(name = "study_title", nullable = false, length = 255)
    private String title;

    // 본문은 길어질 수 있으므로 LOB으로 저장
    @Lob
    @Column(name = "study_content")
    private String content;

    // 진행 방식(온라인 / 오프라인)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyMethod method;

    // 오프라인 스터디일 경우 지역 정보
    @Column(length = 50)
    private String region;

    // 모집 정원
    // TODO 생성 시점에 1 이상인지 검증하는 로직 추가 예정
    @Column(nullable = false)
    private int capacity;

    // 모집 상태(OPEN / CLOSED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudyStatus studyStatus;

    // 최초 생성 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 마지막 수정 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Soft Delete 용 컬럼
    private LocalDateTime deletedAt;

}