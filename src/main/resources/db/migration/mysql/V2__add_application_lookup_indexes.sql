-- 특정 스터디의 승인 인원을 집계할 때 사용할 복합 인덱스
CREATE INDEX idx_application_study_status
    ON application (study_id, application_status);

-- 특정 회원이 특정 스터디에 유효한 신청을 가지고 있는지 확인할 때 사용할 복합 인덱스
CREATE INDEX idx_application_member_study_status
    ON application (member_id, study_id, application_status);