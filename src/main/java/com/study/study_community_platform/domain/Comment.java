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
// 삭제된 댓글은 제외하고 조회
@SQLRestriction("deleted_at is NULL")
public class Comment {

    // 댓글 최대 길이를 한 곳에서 정의
    // static 상수는 JPA가 DB에 저장 x
    public static final int MAX_CONTENT_LENGTH = 1000;

    // 댓글의 기본키 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    // 댓글이 작성된 스터디
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "study_id", nullable = false)
    private Study study;

    // 댓글 작성자
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 댓글 내용
    @Lob
    private String content;

    // 댓글 생성 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 댓글 수정 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // soft delete 처리용 컬럼
    private LocalDateTime deletedAt;

    // 댓글 객체가 저장되기 직전에 생성/수정 시간을 세팅
    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // 댓글 객체 수정 직전에 수정 시간을 갱신
    @PreUpdate
    public void preUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    // 댓글 생성 메서드
    public static Comment createComment(Member member, Study study, String content){

        // 댓글 내용이 검증되지 않으면 예외 발생
        validateContent(content);

        Comment comment = new Comment();
        comment.member = member;
        comment.study = study;
        comment.content = content;
        return comment;
    }

    // 댓글 내용 수정 메서드
    public void changeCommentInfo(String content) {
        // 내용 수정 전에 검증 -> 실패하면 원문 유지
        validateContent(content);

        this.content = content;
    }

    // 댓글 내용 검증
    private static void validateContent(String content){
        // null 이거나 빈 문자열 또는 공백일 경우
        if(content == null || content.isBlank()){
            throw new BusinessRuleException("댓글 내용을 입력해주세요.");
        }

        // 댓글 내용이 1000자 초과일 경우
        if(content.length() > MAX_CONTENT_LENGTH){
            throw new BusinessRuleException("댓글은 " + MAX_CONTENT_LENGTH + "자 이하로 입력해주세요.");
        }
    }

    // 댓글 삭제 (Soft Delete) 처리
    public void withdraw(){
        this.deletedAt = LocalDateTime.now();
    }
}