package com.study.study_community_platform.domain;

import com.study.study_community_platform.exception.BusinessRuleException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

import static jakarta.persistence.FetchType.LAZY;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_at is NULL")
public class Study {

    public static final int MAX_TITLE_LENGTH = 255;
    public static final int MAX_REGION_LENGTH = 50;
    public static final int MAX_CONTENT_LENGTH = 10000;

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
    @Column(name = "study_title", nullable = false, length = MAX_TITLE_LENGTH)
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
    @Column(length = MAX_REGION_LENGTH)
    private String region;

    // 모집 정원
    @Column(nullable = false)
    private int capacity;

    // 모집 상태(OPEN / CLOSED)
    @Enumerated(EnumType.STRING)
    @Column(name = "study_status", nullable = false)
    private StudyStatus studyStatus;

    // 최초 생성 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 마지막 수정 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Soft Delete 용 컬럼
    private LocalDateTime deletedAt;

    // 스터디 객체가 저장되기 직전에 생성/수정 시간을 세팅
    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 스터디 객체 수정 직전에 수정 시간을 갱신
    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    // 스터디 생성 메서드
    // 스터디 생성 시 필요한 규칙을 엔티티 내부에서 관리
    public static Study createStudy(Member member, String title, String content,
                                    StudyMethod method, String region, int capacity) {

        validateStudyInfo(title, content, method, region, capacity);

        Study study = new Study();

        // 스터디 기본 정보 설정
        study.member = member;
        study.title = title;
        study.content = content;
        study.method = method;
        study.capacity = capacity;

        // 온라인 스터디는 지역 정보가 필요 없음
        // 오프라인 스터디만 지역 정보 저장
        if (method == StudyMethod.ONLINE) {
            study.region = null;
        } else {
            study.region = region;
        }

        // 스터디 생성 시 기본 상태는 OPEN
        study.studyStatus = StudyStatus.OPEN;

        return study;
    }

    // 스터디 정보 수정 메서드
    public void changeStudyInfo(String title, String content,
                                StudyMethod method, String region, int capacity, long approvedCount) {

        validateStudyInfo(title, content, method, region, capacity);

        validateCapacity(capacity, approvedCount);

        this.title = title;
        this.content = content;
        this.method = method;

        if (method == StudyMethod.ONLINE) {
            this.region = null;
        } else {
            this.region = region;
        }

        this.capacity = capacity;
        updateRecruitmentStatus(approvedCount);

    }

    private static void validateStudyInfo(String title, String content,
                                          StudyMethod method, String region, int capacity){
        if(title == null || title.isBlank()){
            throw new BusinessRuleException("스터디 제목은 필수입니다.");
        }

        if (title.length() > MAX_TITLE_LENGTH) {
            throw new BusinessRuleException("스터디 제목은 " + MAX_TITLE_LENGTH + "자 이하로 입력해주세요.");
        }

        if (content == null || content.isBlank()) {
            throw new BusinessRuleException("스터디 소개는 필수입니다.");
        }

        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessRuleException("스터디 소개는 " + MAX_CONTENT_LENGTH + "자 이하로 입력해주세요.");
        }

        if (method == null) {
            throw new BusinessRuleException("진행 방식을 선택해주세요.");
        }

        if (region != null && region.length() > MAX_REGION_LENGTH) {
            throw new BusinessRuleException("지역은 " + MAX_REGION_LENGTH + "이하로 입력해주세요.");
        }

        // 오프라인일 때만 지역 검사
        if (method == StudyMethod.OFFLINE
                && (region == null || region.isBlank())) {
            throw new BusinessRuleException("지역을 입력해주세요.");
        }

        if (capacity < 1) {
            throw new BusinessRuleException("모집 인원은 1명 이상이어야 합니다.");
        }
    }

    // 스터디 삭제 (Soft Delete) 처리
    public void withdraw(){
        this.deletedAt = LocalDateTime.now();
    }

    // 정원에 따른 모집 상태 변경
    public void updateRecruitmentStatus(long approvedCount) {
        validateCapacity(this.capacity, approvedCount);

        // 승인 인원과 정원이 같으면 마감
        // 정원에 여유가 있으면 모집 상태로 전환
        this.studyStatus = (approvedCount == this.capacity) ? StudyStatus.CLOSED : StudyStatus.OPEN;
    }

    // 스터디 인원 검증
    private static void validateCapacity(int capacity, long approvedCount) {
        if (capacity < 1) {
            throw new BusinessRuleException("모집 인원은 1명 이상이어야 합니다.");
        }

        if (approvedCount < 0) {
            throw new BusinessRuleException("승인 인원은 0명 이상이어야 합니다.");
        }

        if (capacity < approvedCount) {
            throw new BusinessRuleException(
                    "모집 정원은 현재 승인 인원("
                            + approvedCount
                            + "명) 이상이어야 합니다."
            );
        }
    }
}