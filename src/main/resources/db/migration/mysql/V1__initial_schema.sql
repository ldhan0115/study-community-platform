-- 최초의 빈 MySQL 스키마에 한 번 적용합니다.
-- 적용 후에는 이 파일을 수정하지 않고 V2, V3 등 새 버전을 추가합니다.

CREATE TABLE member (
    member_id BIGINT NOT NULL AUTO_INCREMENT,
    login_id VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    PRIMARY KEY (member_id),

    CONSTRAINT uq_member_login_id UNIQUE (login_id),
    CONSTRAINT uq_member_email UNIQUE (email),
    CONSTRAINT uq_member_nickname UNIQUE (nickname)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_as_cs;


CREATE TABLE study (
    study_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,

    study_title VARCHAR(255) NOT NULL,
    study_content LONGTEXT NULL,

    method ENUM('OFFLINE', 'ONLINE') NOT NULL,
    region VARCHAR(50) NULL,
    capacity INT NOT NULL,
    study_status ENUM('CLOSED', 'OPEN') NOT NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    PRIMARY KEY (study_id),

    CONSTRAINT fk_study_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id),

    INDEX idx_study_title (study_title)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_as_cs;


CREATE TABLE application (
    application_id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    study_id BIGINT NOT NULL,

    message VARCHAR(255) NULL,
    application_status
        ENUM('APPROVED', 'CANCELED', 'PENDING', 'REJECTED') NOT NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    PRIMARY KEY (application_id),

    CONSTRAINT fk_application_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id),

    CONSTRAINT fk_application_study
        FOREIGN KEY (study_id)
        REFERENCES study(study_id),

    INDEX idx_application_status_created_at (
        application_status,
        created_at
    )
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_as_cs;


CREATE TABLE comment (
    comment_id BIGINT NOT NULL AUTO_INCREMENT,
    study_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,

    -- Comment 엔티티가 실제로 사용하는 컬럼명
    content LONGTEXT NULL,

    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,

    PRIMARY KEY (comment_id),

    CONSTRAINT fk_comment_study
        FOREIGN KEY (study_id)
        REFERENCES study(study_id),

    CONSTRAINT fk_comment_member
        FOREIGN KEY (member_id)
        REFERENCES member(member_id),

    INDEX idx_comment_study (study_id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_0900_as_cs;