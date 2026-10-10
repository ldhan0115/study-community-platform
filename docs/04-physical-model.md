# 물리적 모델링 (Physical Data Modeling)

현재 MySQL 실행 스키마의 테이블, 컬럼, 제약조건과 인덱스를 정리한다.

스키마 생성의 기준은 [V1__initial_schema.sql](../src/main/resources/db/migration/mysql/V1__initial_schema.sql)이며, Hibernate는 엔티티와 DB의 매핑을 검증한다.

## 1. 설계 기준

| 항목 | 설정 |
| --- | --- |
| DBMS | MySQL 8.4 |
| 엔진 | InnoDB |
| 문자 집합 | utf8mb4 |
| Collation | utf8mb4_0900_as_cs |
| 기본 키 | BIGINT, NOT NULL, AUTO_INCREMENT |
| 시간 | DATETIME(6) |
| 상태·진행 방식 | MySQL ENUM, Java EnumType.STRING |
| 스키마 관리 | Flyway |
| Hibernate DDL 정책 | MySQL에서는 validate |

문자열 비교는 대소문자를 구분하는 collation을 사용한다. 생성·수정 시간은 JPA의 `@PrePersist`, `@PreUpdate`에서 설정하며, DDL에 자동 시간 기본값이나 `ON UPDATE`는 정의하지 않았다.

회원·스터디·댓글의 삭제는 `deleted_at`을 기록하는 논리적 삭제다. 신청은 상태 변경으로 처리 이력을 보존한다.

## 2. 테이블 정의

아래 NULL 허용 여부는 실제 DB 스키마 기준이다.

### 2.1 member

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| member_id | BIGINT | PK, NOT NULL, AUTO_INCREMENT | 회원 식별자 |
| login_id | VARCHAR(50) | NOT NULL, UNIQUE | 로그인 ID |
| password | VARCHAR(255) | NOT NULL | 비밀번호 해시 |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 이메일 |
| nickname | VARCHAR(50) | NOT NULL, UNIQUE | 표시 이름 |
| created_at | DATETIME(6) | NOT NULL | 생성일 |
| updated_at | DATETIME(6) | NOT NULL | 수정일 |
| deleted_at | DATETIME(6) | NULL 허용 | 탈퇴일 |

| UNIQUE 제약 이름 | 컬럼 |
| --- | --- |
| uq_member_login_id | login_id |
| uq_member_email | email |
| uq_member_nickname | nickname |

UNIQUE 제약은 탈퇴한 회원의 행에도 적용된다. 탈퇴한 계정의 로그인 ID·이메일·닉네임은 재사용하지 않는다.

### 2.2 study

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| study_id | BIGINT | PK, NOT NULL, AUTO_INCREMENT | 스터디 식별자 |
| member_id | BIGINT | FK, NOT NULL | 작성자 |
| study_title | VARCHAR(255) | NOT NULL | 제목 |
| study_content | LONGTEXT | NULL 허용 | 본문 |
| method | ENUM('OFFLINE', 'ONLINE') | NOT NULL | 진행 방식 |
| region | VARCHAR(50) | NULL 허용 | 오프라인 지역 |
| capacity | INT | NOT NULL | 승인 가능한 신청자 정원 |
| study_status | ENUM('CLOSED', 'OPEN') | NOT NULL | 모집 상태 |
| created_at | DATETIME(6) | NOT NULL | 생성일 |
| updated_at | DATETIME(6) | NOT NULL | 수정일 |
| deleted_at | DATETIME(6) | NULL 허용 | 삭제일 |

`capacity`에는 DB CHECK 제약이 없다. 최소 정원, 승인 인원보다 작은 정원으로의 수정 방지, 모집 상태 갱신은 애플리케이션에서 처리한다.

본문의 저장 타입과 사용자 입력 길이는 별도로 관리한다.

```java
// MySQL LONGTEXT와 일치하는 LOB 저장 길이를 지정한다.
@Lob
@Column(name = "study_content", length = Length.LONG32)
private String content;
```

`Length`는 `org.hibernate.Length`이며, 실제 사용자 입력은 폼과 도메인에서 최대 10,000자로 제한한다.

### 2.3 application

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| application_id | BIGINT | PK, NOT NULL, AUTO_INCREMENT | 신청 식별자 |
| member_id | BIGINT | FK, NOT NULL | 신청자 |
| study_id | BIGINT | FK, NOT NULL | 신청 대상 |
| message | VARCHAR(255) | NULL 허용 | 신청 메시지 |
| application_status | ENUM('APPROVED', 'CANCELED', 'PENDING', 'REJECTED') | NOT NULL | 신청 상태 |
| created_at | DATETIME(6) | NOT NULL | 신청일 |
| updated_at | DATETIME(6) | NOT NULL | 수정일 |

- 상태의 기본값 PENDING은 `Application.createApplication()`에서 설정한다. DDL에는 기본값이 없다.
- `(member_id, study_id)`에 UNIQUE 제약을 두지 않는다.
- 취소·거절 후 재신청은 기존 행을 보존하고 새 행으로 저장한다.
- PENDING·APPROVED가 있는지 검사하는 중복 신청 정책은 서비스에 구현되어 있다.
- ENUM은 상태값의 집합을 정의하며, 허용되는 상태 전이 순서는 도메인에서 검사한다.

### 2.4 comment

| 컬럼 | 타입 | 제약조건 | 설명 |
| --- | --- | --- | --- |
| comment_id | BIGINT | PK, NOT NULL, AUTO_INCREMENT | 댓글 식별자 |
| study_id | BIGINT | FK, NOT NULL | 소속 스터디 |
| member_id | BIGINT | FK, NOT NULL | 작성자 |
| content | LONGTEXT | NULL 허용 | 댓글 내용 |
| created_at | DATETIME(6) | NOT NULL | 생성일 |
| updated_at | DATETIME(6) | NOT NULL | 수정일 |
| deleted_at | DATETIME(6) | NULL 허용 | 삭제일 |

내용 컬럼명은 `content`이다. `Comment.content`는 `@Lob`으로 매핑하고, 입력 폼과 도메인에서 최대 1,000자로 제한한다.

## 3. 외래키

| 외래키 이름 | 자식 컬럼 | 참조 컬럼 |
| --- | --- | --- |
| fk_study_member | study.member_id | member.member_id |
| fk_application_member | application.member_id | member.member_id |
| fk_application_study | application.study_id | study.study_id |
| fk_comment_study | comment.study_id | study.study_id |
| fk_comment_member | comment.member_id | member.member_id |

모든 외래키 컬럼은 NOT NULL이다. V1 DDL에는 ON DELETE CASCADE를 정의하지 않았다.

외래키는 부모 행의 존재를 보장한다. 부모의 `deleted_at`을 기준으로 하는 활성 여부와 사용자 권한은 서비스·조회 로직에서 검사한다.

## 4. 인덱스

기본 키와 UNIQUE 제약 외에 V1 DDL에서 명시한 일반 인덱스는 다음과 같다.

| 인덱스명 | 테이블 | 컬럼 순서 | 설계 의도 |
| --- | --- | --- | --- |
| idx_study_title | study | study_title | 제목 조건 조회 |
| idx_application_status_created_at | application | application_status, created_at | 상태·생성일 조건 조회 |
| idx_comment_study | comment | study_id | 스터디별 댓글 조회 |

제목 검색·상태와 기간별 검색 기능 및 각 쿼리에서의 인덱스 효과를 모두 구현·검증했다는 의미는 아니다. 실제 SQL과 실행 계획을 확인해 필요한 인덱스를 보완할 예정이다.

## 5. DB 제약과 애플리케이션 검증

| 항목 | DB 저장 구조 | 애플리케이션 입력·처리 규칙 |
| --- | --- | --- |
| 로그인 ID | VARCHAR(50), NOT NULL, UNIQUE | 빈 값·공백만 입력 불가, 4~20자 |
| 비밀번호 | VARCHAR(255), NOT NULL | 입력은 빈 값·공백만 입력 불가·8~20자, 저장은 해시 |
| 이메일 | VARCHAR(100), NOT NULL, UNIQUE | 필수, 이메일 형식, 최대 100자 |
| 닉네임 | VARCHAR(50), NOT NULL, UNIQUE | 빈 값·공백만 입력 불가, 2~20자 |
| 스터디 제목 | VARCHAR(255), NOT NULL | 빈 값·공백만 입력 불가, 최대 255자 |
| 스터디 본문 | LONGTEXT, NULL 허용 | 빈 값·공백만 입력 불가, 최대 10,000자 |
| 지역 | VARCHAR(50), NULL 허용 | 최대 50자, OFFLINE은 필수, ONLINE은 NULL 저장 |
| 모집 정원 | INT, NOT NULL | 1 이상, 수정 시 승인 인원 이상 |
| 신청 메시지 | VARCHAR(255), NULL 허용 | 빈 값·공백만 입력 불가, 최대 255자 |
| 댓글 내용 | LONGTEXT, NULL 허용 | 빈 값·공백만 입력 불가, 최대 1,000자 |

현재 `study_content`, `message`, `content`는 DB에서 NULL을 허용한다. 애플리케이션의 필수 입력 정책을 DB NOT NULL 제약으로도 강화하려면 기존 데이터를 점검하고 후속 마이그레이션을 추가해야 한다.

승인 인원은 `APPROVED` 신청 수로 계산하며 별도 집계 컬럼을 두지 않는다. 정원 초과, 유효 상태 기준 중복 신청, 상태 변경 충돌에 대한 동시성 제어는 아직 추가 과제다.

## 6. 논리적 삭제

| 테이블 | 삭제 표현 | 애플리케이션 처리 |
| --- | --- | --- |
| member | deleted_at 기록 | 로그인·활성 회원 조회 제외, 변경 요청의 활성 회원 검사 |
| study | deleted_at 기록 | 일반 엔티티 조회 제외, 관련 신청·댓글 변경 차단 |
| comment | deleted_at 기록 | 일반 엔티티 조회 제외, 수정·삭제 차단 |
| application | 상태 변경 | 취소는 CANCELED, 별도 deleted_at 컬럼 없음 |

- Study와 Comment의 일반 엔티티 조회에는 `@SQLRestriction`을 사용한다.
- Member에는 전역 삭제 필터를 적용하지 않고 활성 회원 조회 메서드를 구분한다.
- 탈퇴한 작성자의 기존 댓글·스터디·신청 연관관계를 보존한다.
- 삭제된 스터디에 연결된 신청은 내 신청 목록에서 제외한다.
- 생성일·수정일은 JPA 생명주기 콜백, 삭제일은 도메인의 삭제 메서드에서 설정한다.

## 7. 실행 환경과 Flyway

| 구분 | DB | 프로필 | DDL 관리 |
| --- | --- | --- | --- |
| 로컬 개발 | study_platform · localhost:3307 | local | Flyway + Hibernate validate |
| MySQL 테스트 | study_platform_test · localhost:3308 | mysql-test | Flyway + Hibernate validate |
| 기본 테스트 | 인메모리 H2 | test | Hibernate create-drop, Flyway 비활성 |
| 운영용 설정 | 환경변수로 지정한 MySQL | prod | Flyway + Hibernate validate |

- MySQL 마이그레이션 경로: `classpath:db/migration/mysql`
- 초기 마이그레이션: `V1__initial_schema.sql`
- `flyway_schema_history`는 Flyway가 적용 이력과 체크섬을 관리하는 테이블이다.
- 적용 완료된 파일은 수정하지 않고 변경 사항을 V2 이후의 마이그레이션으로 추가한다.
- `spring.sql.init.mode=never`로 별도의 SQL 초기화를 비활성화한다.
- Flyway의 `clean-disabled=true`, `baseline-on-migrate=false`를 사용한다.
- V1은 빈 MySQL DB를 대상으로 하며 기존 H2 데이터의 자동 이전은 포함하지 않는다.
- `database/ddl.sql`은 실제 마이그레이션 경로를 안내하는 파일이다.

`TestDataInit`은 `local` 프로필에서 `app.seed.enabled=true`이고 회원 테이블이 비어 있을 때만 샘플을 생성한다. 샘플 데이터 생성과 Flyway의 테이블 생성은 별도로 관리한다.

## 8. 검증과 관련 문서

`MySqlPersistenceTest`에는 스키마 검증, Flyway 재실행, 한글·이모지·Enum 저장, 회원 UNIQUE 제약, 존재하지 않는 스터디를 참조하는 댓글의 FK 위반을 확인하는 테스트가 있다.

Hibernate의 매핑 검증과 실제 INSERT 시 DB 제약 검증을 함께 사용한다.

- [논리적 모델과 관계 ERD](./03-logical-model.md)
- [Flyway V1 DDL](../src/main/resources/db/migration/mysql/V1__initial_schema.sql)
- [실행 및 테스트 방법](../README.md)