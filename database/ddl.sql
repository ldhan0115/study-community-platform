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
        REFERENCES study(study_id),

    CONSTRAINT uq_member_study
        UNIQUE (member_id, study_id)
);


-- 인덱스
CREATE INDEX idx_application_status_created_at
ON application(application_status, created_at);


-- =====================================================
-- POST
-- =====================================================

CREATE TABLE post (
    post_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    member_id BIGINT NOT NULL,

    post_title VARCHAR(255) NOT NULL,
    post_content TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    CONSTRAINT fk_post_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id)
);


-- 인덱스
CREATE INDEX idx_post_title
ON post(post_title);


-- =====================================================
-- COMMENT
-- =====================================================

CREATE TABLE comment (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,

    post_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,

    comment_content TEXT NULL,

    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,

    CONSTRAINT fk_comment_post
        FOREIGN KEY (post_id)
        REFERENCES post(post_id),

    CONSTRAINT fk_comment_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id)
);


-- 인덱스
CREATE INDEX idx_comment_post
ON comment(post_id);