package com.study.study_community_platform.exception;

// 요청 형식은 정상이지만 현재 비즈니스 규칙상 수행할 수 없을 때 사용하는 예외
// GlobalExceptionHandler에서 HTTP 400으로 처리
public class BusinessRuleException extends RuntimeException{

    public BusinessRuleException(String message) {
        super(message);
    }
}
