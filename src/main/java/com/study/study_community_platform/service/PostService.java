package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Post;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    // 게시물 등록
    @Transactional
    public Long registerPost(Long memberId, String title, String content) {

        // 게시글 작성 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        // Post 객체 생성 메서드를 사용해 생성 규칙을 한 곳에서 관리
        Post post = Post.createPost(member, title, content);

        postRepository.save(post);
        return post.getId();
    }

    // 게시물 단건 조회
    public Post findPost(Long postId) {
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));
    }

    // 특정 회원의 게시물 목록 조회
    public List<Post> findPostsByMember(Long memberId) {
        return postRepository.findByMemberId(memberId);
    }

    // 전체 게시물 조회
    public List<Post> findPosts() {
        return postRepository.findAll();
    }

    // 게시물 수정
    @Transactional
    public void updatePost(Long postId, String title, String content) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시물입니다."));

        post.changePostInfo(title, content);
    }

    // 게시물 삭제
    @Transactional
    public void deletePost(Long postId) {
        postRepository.delete(postId);
    }
}