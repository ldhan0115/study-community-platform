package com.study.study_community_platform.exception;

// 로그인은 했지만 해당 작업을 수행할 권한이 없을 때 사용하는 예외
// GlobalExceptionHandler에서 HTTP 403으로 처리
public class ForbiddenOperationException extends RuntimeException{

    public ForbiddenOperationException(String message) {
        super(message);
    }
}
