package com.study.study_community_platform.controller.web.comment;

import com.study.study_community_platform.domain.Comment;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 사용자의 댓글 입력을 받을 DTO
public class CommentForm {

    // 값 누락, 빈 문자열, 공백만 있는 입력 거부
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(
            max = Comment.MAX_CONTENT_LENGTH,
            message = "댓글은 {max}자 이하로 입력해주세요."
    )
    private String content;
}
