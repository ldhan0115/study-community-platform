-- =====================================================
-- Study Community Platform Database Schema
-- =====================================================

-- =====================================================
-- MEMBER
-- =====================================================

CREATE TABLE member (
    member_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    login_id VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    CONSTRAINT uq_member_login_id UNIQUE (login_id),
    CONSTRAINT uq_member_email UNIQUE (email),
    CONSTRAINT uq_member_nickname UNIQUE (nickname)
);


-- =====================================================
-- STUDY
-- =====================================================

CREATE TABLE study (
    study_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id BIGINT NOT NULL,
    study_title VARCHAR(255) NOT NULL,
    study_content TEXT NULL,
    method VARCHAR(20) NOT NULL,
    region VARCHAR(50) NULL,
    capacity INT NOT NULL,
    study_status VARCHAR(20) NOT NULL DEFAULT 'OPEN',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    CONSTRAINT fk_study_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id)
);


-- 인덱스
CREATE INDEX idx_study_title
ON study(study_title);


-- =====================================================
-- APPLICATION
-- =====================================================

CREATE TABLE application (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    member_id BIGINT NOT NULL,
    study_id BIGINT NOT NULL,
    message VARCHAR(255) NULL,
    application_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_application_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id),

    CONSTRAINT fk_application_study
        FOREIGN KEY (study_id)
        REFERENCES study(study_id)
);


-- 인덱스
CREATE INDEX idx_application_status_created_at
ON application(application_status, created_at);

-- =====================================================
-- COMMENT
-- =====================================================

CREATE TABLE comment (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    study_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    comment_content TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    CONSTRAINT fk_comment_study
        FOREIGN KEY (study_id)
        REFERENCES study(study_id),

    CONSTRAINT fk_comment_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id)
);


-- 인덱스
CREATE INDEX idx_comment_study
ON comment(study_id);