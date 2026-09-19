package com.study.study_community_platform.controller.advice;

import com.study.study_community_platform.exception.BusinessRuleException;
import com.study.study_community_platform.exception.ForbiddenOperationException;
import com.study.study_community_platform.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 존재하지 않는 리소스를 요청한 경우 처리
    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView handleNotFound(ResourceNotFoundException exception){
        log.warn("존재하지 않는 리소스 요청: {}", exception.getMessage());

        return createErrorView(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없습니다.", exception.getMessage());
    }

    // 로그인한 사용자가 권한 없는 작업을 요청한 경우 처리
    @ExceptionHandler(ForbiddenOperationException.class)
    public ModelAndView handleForbidden(ForbiddenOperationException exception){
        log.warn("권한 없는 작업 요청: {}", exception.getMessage());

        return createErrorView(HttpStatus.FORBIDDEN, "요청을 수행할 권한이 없습니다.", exception.getMessage());
    }

    // 비즈니스 규칙을 위반한 요청 처리
    @ExceptionHandler(BusinessRuleException.class)
    public ModelAndView handleBusinessRule(BusinessRuleException exception){
        log.warn("비즈니스 규칙 위반: {}", exception.getMessage());

        return createErrorView(HttpStatus.BAD_REQUEST, "요청을 처리할 수 없습니다.", exception.getMessage());
    }

    // 모든 오류 화면에서 공통으로 사용할 ModelAndView 생성
    // setStatus()를 호출하여 화면만 보여주는 것이 아니라 실제 HTTP 상태도 400·403·404으로 반환
    private ModelAndView createErrorView(HttpStatus status, String title, String message){
        ModelAndView modelAndView = new ModelAndView("error/error");

        modelAndView.setStatus(status);
        modelAndView.addObject("status", status.value());

        modelAndView.addObject("error", status.getReasonPhrase());

        modelAndView.addObject("title", title);

        modelAndView.addObject("message", message);

        return modelAndView;
    }
}
