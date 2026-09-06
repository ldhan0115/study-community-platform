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
    @Column(name = "application_status", nullable = false)
    private ApplicationStatus status;

    // 신청 생성 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 신청 정보 수정 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 신청 객체가 저장되기 직전에 생성/수정 시간을 세팅
    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 신청 객체 수정 직전에 수정 시간을 갱신
    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    // 신청 생성 메서드
    public static Application createApplication(Member member, Study study, String message){
        Application application = new Application();

        application.member = member;
        application.study = study;
        application.message = message;
        application.status = ApplicationStatus.PENDING;

        return application;
    }

    // 신청 승인
    public void approve() {
        changeStatus(ApplicationStatus.APPROVED);
    }

    // 신청 거절
    public void reject() {
        changeStatus(ApplicationStatus.REJECTED);
    }

    // 신청 취소
    public void cancel() {
        changeStatus(ApplicationStatus.CANCELED);
    }

    // 새로 생성된 PENDING 신청만 승인·거절·취소 가능, 이미 처리가 끝난 신청은 다른 상태로 변경 x
    private void changeStatus(ApplicationStatus nextStatus){
        validatePendingStatus();
        this.status = nextStatus;
    }

    // 신청이 아직 처리되지 않은 PENDING 상태인지 확인
    public void validatePendingStatus(){
        if(this.status != ApplicationStatus.PENDING){
            throw new IllegalStateException("대기 중인 신청만 상태를 변경할 수 있습니다.");
        }
    }

}

