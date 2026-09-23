package com.study.study_community_platform.controller;

import com.study.study_community_platform.config.security.LoginMemberPrincipal;
import com.study.study_community_platform.domain.Comment;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.repository.CommentRepository;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class CommentControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager em;

    private Member author;
    private Study study;
    private LoginMemberPrincipal principal;

    @BeforeEach
    void setUp(){
        author = memberRepository.save(Member.createMember(
                "tester",
                passwordEncoder.encode("password"),
                "tester@test.com",
                "테스터"
        ));

        study = studyRepository.save(Study.createStudy(
                author,
                "테스트 댓글",
                "내용",
                StudyMethod.ONLINE,
                null,
                5
        ));

        principal = LoginMemberPrincipal.from(author);
    }

    @ParameterizedTest(name = "등록 유지 및 미저장: {1}")
    @MethodSource("invalidContents")
    void invalidCreateDoesNotSave(String content) throws Exception {
        long beforeCount = commentRepository.count();

        mockMvc.perform(commentPost(createUrl(), content))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(detailUrl()))
                .andExpect(flash().attributeExists("errorMessage"));

        em.flush();
        em.clear();

        // 잘못된 요청으로 댓글이 추가되지 않았는지 확인
        assertThat(commentRepository.count()).isEqualTo(beforeCount);
    }

    @ParameterizedTest(name = "수정 오류 및 DB 원문 유지: {1}")
    @MethodSource("invalidContents")
    void invalidEditKeepsSavedContent(String content) throws Exception {
        Comment comment = commentRepository.save(Comment.createComment(author, study, "기존 댓글"));
        Long commentId = comment.getId();

        em.flush();
        em.clear();

        mockMvc.perform(commentPost(editUrl(commentId), content))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(detailUrl()))
                .andExpect(flash().attributeExists("errorMessage"))
                .andExpect(flash().attribute("editingCommentId", commentId));

        em.flush();
        em.clear();

        Comment unchanged = commentRepository.findById(commentId).orElseThrow();

        assertThat(unchanged.getContent()).isEqualTo("기존 댓글");
    }

    @ParameterizedTest(name = "허용 길이 등록: {1}")
    @MethodSource("validContents")
    void validCreateSavesContent(String content) throws Exception {
        long beforeCount = commentRepository.count();

        mockMvc.perform(commentPost(createUrl(), content))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(detailUrl()));

        em.flush();
        em.clear();

        assertThat(commentRepository.count()).isEqualTo(beforeCount + 1);
        List<Comment> comments = commentRepository.findByStudyId(study.getId());

        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getContent()).isEqualTo(content);
    }

    @ParameterizedTest(name = "허용 길이 수정: {1}")
    @MethodSource("validContents")
    void validEditUpdatesContent(String content) throws Exception {
        Comment comment = commentRepository.save(
                Comment.createComment(author, study, "기존 댓글")
        );

        Long commentId = comment.getId();
        long beforeCount = commentRepository.count();

        em.flush();
        em.clear();

        mockMvc.perform(commentPost(editUrl(commentId), content))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(detailUrl()));

        em.flush();
        em.clear();

        // 수정으로 새 댓글이 생기지 않았는지도 확인
        assertThat(commentRepository.count()).isEqualTo(beforeCount);

        Comment updated = commentRepository.findById(commentId)
                .orElseThrow();

        assertThat(updated.getContent()).isEqualTo(content);
    }

    static Stream<String> invalidContents(){
        return Stream.of(
                null,
                "",
                "  ",
                "\t\n",
                "a".repeat(1001)
        );
    }

    static Stream<String> validContents(){
        return Stream.of(
                "a",
                "a".repeat(1000)
        );
    }

    private MockHttpServletRequestBuilder commentPost(String url, String content){
        MockHttpServletRequestBuilder request = post(url)
                // 인증된 사용자의 요청으로 구성
                .with(user(principal))
                .with(csrf());

        if(content != null){
            request.param("content", content);
        }

        return request;
    }


    private String detailUrl(){
        return "/studies/" + study.getId();
    }

    private String createUrl(){
        return detailUrl() + "/comments";
    }

    private String editUrl(Long commentId){
        return detailUrl() + "/comments/" + commentId + "/edit";
    }
}
