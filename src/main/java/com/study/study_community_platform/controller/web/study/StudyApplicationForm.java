package com.study.study_community_platform.controller.web.study;

import com.study.study_community_platform.domain.Application;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudyApplicationForm {

    @NotBlank(message = "신청 메시지를 입력해주세요.")
    @Size(max = Application.MAX_MESSAGE_LENGTH, message = "신청 메시지는 255자 이하로 입력해주세요.")
    private String message;
}
