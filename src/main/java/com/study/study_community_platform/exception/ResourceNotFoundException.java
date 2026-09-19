package com.study.study_community_platform.exception;

// 요청한 회원, 스터디, 신청, 댓글 등이 존재하지 않을 때 사용하는 예외
// GlobalExceptionHandler에서 HTTP 404로 처리
public class ResourceNotFoundException extends RuntimeException{

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
