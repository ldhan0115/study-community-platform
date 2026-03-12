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
public class Post {

    // 게시글의 기본 키 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    // 게시글 작성자
    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 게시글 제목
    @Column(name = "post_title", nullable = false, length = 255)
    private String title;

    // 게시글 본문
    // 내용이 길어질 수 있어 LOB으로 저장
    @Lob
    @Column(name = "post_content")
    private String content;

    // 게시글 생성 시간
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // 게시글 수정 시간
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // soft delete 처리용 컬럼
    private LocalDateTime deletedAt;
}