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
@Table(
        // 동일 회원이 동일 스터디에 중복 신청하는 것을 방지
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_member_id_study_id", columnNames = {"member_id", "study_id"})
        }
)
public class Application {

    // 신청의 기본키 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "application_id")
    private Long id;

    // 스터디 신청을 한 회원
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 신청 대상 스터디
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    // 신청 시 남기는 메시지
    @Column(length = 255)
    private String message;

    // 신청 상태
    // PENDING → 승인 대기
    // APPROVED → 승인
    // REJECTED → 거절
    // CANCELED → 취소
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    // 신청 생성 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 신청 정보 수정 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}

