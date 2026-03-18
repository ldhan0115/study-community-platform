package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Comment;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Post;
import com.study.study_community_platform.repository.CommentRepository;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;

    // 댓글 등록
    @Transactional
    public Long registerComment(Long memberId, Long postId, String content) {

        // 댓글 작성 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // 댓글 작성 게시물 존재 여부 확인
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        // Comment 객체 생성 메서드를 사용
        Comment comment = Comment.createComment(member, post, content);

        commentRepository.save(comment);
        return comment.getId();
    }

    // 댓글 단건 조회
    public Comment findComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));
    }

    // 특정 회원의 댓글 목록 조회
    public List<Comment> findCommentsByMember(Long memberId) {
        return commentRepository.findByMemberId(memberId);
    }

    // 특정 게시물의 댓글 목록 조회
    public List<Comment> findCommentsByPost(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    // 전체 댓글 조회
    public List<Comment> findComments() {
        return commentRepository.findAll();
    }

    // 댓글 수정
    @Transactional
    public void updateComment(Long commentId, String content) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 댓글입니다."));

        comment.changeCommentInfo(content);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long commendId) {
        commentRepository.delete(commendId);
    }


}
