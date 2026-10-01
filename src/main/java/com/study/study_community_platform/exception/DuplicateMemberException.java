package com.study.study_community_platform.exception;

public class DuplicateMemberException extends BusinessRuleException{

    // loginId, email, nickname 중 어떤 필드가 중복인지 전달
    private final String field;

    public DuplicateMemberException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
