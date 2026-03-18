package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Comment;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Post;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CommentServiceTest {

    @Autowired CommentService commentService;
    @Autowired MemberRepository memberRepository;
    @Autowired PostRepository postRepository;

    @Test
    void registerComment(){
        // given
        Member member = new Member("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Post post = Post.createPost(member, "오늘 공부한 영어 문장", "I'm a student");
        postRepository.save(post);

        //when
        Long commentId = commentService.registerComment(member.getId(), post.getId(), "화이팅!!");
        Comment findComment = commentService.findComment(commentId);

        //then
        assertThat(findComment.getId()).isEqualTo(commentId);
        assertThat(findComment.getMember()).isEqualTo(member);
        assertThat(findComment.getPost()).isEqualTo(post);
        assertThat(findComment.getContent()).isEqualTo("화이팅!!");
    }

    @Test
    void findCommentsByMember(){
        // given
        Member member = new Member("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Post post = Post.createPost(member, "오늘 공부한 영어 문장", "I'm a student");
        postRepository.save(post);

        Long commentId1 = commentService.registerComment(member.getId(), post.getId(), "화이팅!!");
        Long commentId2 = commentService.registerComment(member.getId(), post.getId(), "힘내용~");

        //when
        List<Comment> commentsByMember = commentService.findCommentsByMember(member.getId());

        //then
        assertThat(commentsByMember.size()).isEqualTo(2);
        assertThat(commentsByMember)
                .extracting(comment -> comment.getMember().getId())
                .containsOnly(member.getId());
    }

    @Test
    void findComments(){
        // given
        Member member1 = new Member("test1", "1234", "test1@gmail.com", "tester1");
        memberRepository.save(member1);

        Member member2 = new Member("test2", "1234", "test2@gmail.com", "tester2");
        memberRepository.save(member2);

        Post post = Post.createPost(member1, "오늘 공부한 영어 문장", "I'm a student");
        postRepository.save(post);

        Long commentId1 = commentService.registerComment(member1.getId(), post.getId(), "화이팅!!");
        Long commentId2 = commentService.registerComment(member2.getId(), post.getId(), "힘내용~");

        Comment comment1 = commentService.findComment(commentId1);
        Comment comment2 = commentService.findComment(commentId2);

        //when
        List<Comment> comments = commentService.findComments();

        //then
        assertThat(comments.size()).isEqualTo(2);
        assertThat(comments).contains(comment1, comment2);
    }

    @Test
    void updateComment(){
        // given
        Member member = new Member("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Post post = Post.createPost(member, "오늘 공부한 영어 문장", "I'm a student");
        postRepository.save(post);

        Long commentId = commentService.registerComment(member.getId(), post.getId(), "화이팅!!");

        //when
        commentService.updateComment(commentId, "Fighting!!");
        Comment findComment = commentService.findComment(commentId);

        //then
        assertThat(findComment.getId()).isEqualTo(commentId);
        assertThat(findComment.getContent()).isEqualTo("Fighting!!");
    }

    @Test
    void deleteComment(){
        // given
        Member member = new Member("test", "1234", "test@gmail.com", "tester");
        memberRepository.save(member);

        Post post = Post.createPost(member, "오늘 공부한 영어 문장", "I'm a student");
        postRepository.save(post);

        Long commentId1 = commentService.registerComment(member.getId(), post.getId(), "화이팅!!");
        Long commentId2 = commentService.registerComment(member.getId(), post.getId(), "Fighting!!");

        //when
        commentService.deleteComment(commentId1);
        List<Comment> comments = commentService.findComments();

        //then
        assertThat(comments.size()).isEqualTo(1);
        assertThat(comments.get(0).getContent()).isEqualTo("Fighting!!");
    }
}