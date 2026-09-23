package com.study.study_community_platform.domain;

import com.study.study_community_platform.exception.BusinessRuleException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// DB 저장이나 로그인을 하지 않는 순수 도메인 테스트
class CommentTest {

    @ParameterizedTest(name = "잘못된 내용으로 생성 거부 : {1}")
    @MethodSource("invalidContents")
    void cannotCreateInvalidComment(String content){
        // 웹 요청을 거치지 않고 직접 생성해도 규칙 적용
        assertThatThrownBy(() -> newComment(content))
                .isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest(name = "잘못된 수정 후 원문 유지: {1}")
    @MethodSource("invalidContents")
    void invalidEditKeepOriginalContent(String content){
        Comment comment = newComment("기존 댓글");

        // 잘못된 내용으로 수정하면 예외 발생 후 원문 유지
        assertThatThrownBy(() -> comment.changeCommentInfo(content))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(comment.getContent()).isEqualTo("기존 댓글");

    }

    // 해당 값들이 테스트 메서드의 content 파라미터로 하나씩 전달
    static Stream<String> invalidContents(){
        return Stream.of(null, "", "  ", "\t\n", "a".repeat(10001));
    }


    private Comment newComment(String content){
        Member member = Member.createMember("tester", "password", "tester@test.com", "테스터");

        Study study = Study.createStudy(member, "테스트 스터디", "내용", StudyMethod.ONLINE, null, 5);

        return Comment.createComment(member, study, content);
    }
}