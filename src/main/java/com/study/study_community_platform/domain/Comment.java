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
public class Comment {

    // 댓글의 기본키 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    // 댓글이 작성된 게시글
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

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
    public static Comment createComment(Member member, Post post, String content){
        Comment comment = new Comment();
        comment.member = member;
        comment.post = post;
        comment.content = content;
        return comment;
    }

    // 댓글 내용 수정 메서드
    public void changeCommentInfo(String content) {
        this.content = content;
    }
}