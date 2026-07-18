package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Post;
import com.study.study_community_platform.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class PostServiceTest {

    @Autowired PostService postService;
    @Autowired MemberRepository memberRepository;

    @Test
    void registerPost(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        //when
        Long postId = postService.registerPost(member.getId(), "오늘 공부한 영어 문장.", "I'm a student");
        Post findPost = postService.findPost(postId);

        //then
        assertThat(findPost.getMember()).isEqualTo(member);
        assertThat(findPost.getTitle()).isEqualTo("오늘 공부한 영어 문장.");
        assertThat(findPost.getContent()).isEqualTo("I'm a student");
    }

    @Test
    void noTitle(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        //when && then
        assertThatThrownBy(() -> postService.registerPost(member.getId(), null, "I'm a student"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시물의 제목은 필수입니다.");
    }

    @Test
    void findPostsByMember(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Long postId1 = postService.registerPost(member.getId(), "오늘 공부한 영어 문장 1", "I'm a student");
        Long postId2 = postService.registerPost(member.getId(), "오늘 공부한 영어 문장 2", "I'm busy");

        //when
        List<Post> postsByMember = postService.findPostsByMember(member.getId());

        //then
        assertThat(postsByMember.size()).isEqualTo(2);
        assertThat(postsByMember)
                .extracting(post -> post.getMember().getId())
                .containsOnly(member.getId());
    }

    @Test
    void findPosts(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Long postId1 = postService.registerPost(member.getId(), "오늘 공부한 영어 문장 1", "I'm a student");
        Long postId2 = postService.registerPost(member.getId(), "오늘 공부한 영어 문장 2", "I'm busy");

        //when
        List<Post> posts = postService.findPosts();
        Post post1 = postService.findPost(postId1);
        Post post2 = postService.findPost(postId2);

        //then
        assertThat(posts.size()).isEqualTo(2);
        assertThat(posts).contains(post1, post2);
    }

    @Test
    void updatePost(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Long postId = postService.registerPost(member.getId(), "오늘 공부한 영어 문장", "I'm a students");

        //when
        postService.updatePost(postId, "오늘 공부한 영어 문장(수정)", "I'm a student");
        Post findPost = postService.findPost(postId);

        //then
        assertThat(findPost.getId()).isEqualTo(postId);
        assertThat(findPost.getTitle()).isEqualTo("오늘 공부한 영어 문장(수정)");
        assertThat(findPost.getContent()).isEqualTo("I'm a student");
    }

    @Test
    void deletePost(){
        // given
        Member member = Member.createMember("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Long postId1 = postService.registerPost(member.getId(), "오늘 공부한 영어 문장 1", "I'm a student");
        Long postId2 = postService.registerPost(member.getId(), "오늘 공부한 영어 문장 2", "I'm busy");

        //when
        postService.deletePost(postId1);
        List<Post> posts = postService.findPosts();

        //then
        assertThat(posts.size()).isEqualTo(1);
        assertThat(posts.get(0).getId()).isEqualTo(postId2);

    }

}