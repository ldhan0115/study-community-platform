package com.study.study_community_platform.domain;

public enum ApplicationStatus {
    // 승인 대기
    PENDING,

    // 승인됨
    APPROVED,

    // 거절됨
    REJECTED,

    // 신청 취소
    CANCELED
}