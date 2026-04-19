package com.study.study_community_platform.controller.web.study;

import com.study.study_community_platform.domain.StudyMethod;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterStudyForm {

    @NotBlank(message = "스터디 제목을 입력해주세요.")
    private String title;

    @Min(value = 2, message = "모집 인원을 입력해주세요.")
    private int capacity;

    @NotNull(message = "진행 방식을 선택해주세요.")
    private StudyMethod method;

    @NotBlank(message = "지역을 입력해주세요")
    private String region;

    @NotBlank(message = "스터디 소개를 입력해주세요.")
    private String content;
}
