# 물리적 모델링 (Physical Data Modeling)

물리적 모델링 단계에서는 논리적 모델링을 바탕으로 실제 DBMS(MySQL)에 적용할 수 있도록
**테이블명, 컬럼명, 데이터 타입, 제약조건, 인덱스**를 구체적으로 설계한다.

---

# 1. 설계 기준

- DBMS: MySQL
- 문자 집합: UTF-8 계열 사용 권장
- 기본 키는 `BIGINT AUTO_INCREMENT` 대리키를 사용한다.
- 생성일/수정일/삭제일 컬럼을 통해 데이터 이력을 관리한다.
- 삭제는 물리 삭제보다 **소프트 삭제(`deleted_at`)**를 우선 고려한다.
- 상태값은 문자열로 저장하되, 애플리케이션 또는 DB 제약조건을 통해 허용 범위를 관리한다.

---

# 2. 테이블 목록

| 테이블명 | 설명 |
|------|------|
| member | 회원 정보 저장 |
| study | 스터디 모집 정보 저장 |
| application | 회원의 스터디 신청 정보 저장 |
| comment | 게시글 댓글 저장 |

---

# 3. 테이블 정의서

## 3.1 member

### 테이블 정보

| 항목 | 내용 |
|------|------|
| 테이블 한글명 | 회원 |
| 테이블 영문명 | member |
| 테이블 설명 | 서비스를 사용하는 회원 정보를 저장 |

### 컬럼 정의

| No. | 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 비고 |
|---|--------|---|---|---|---|
| 1 | 회원 ID  | member_id | BIGINT | PK, AUTO_INCREMENT | 회원의 고유 식별자 |
| 2 | 로그인 ID | login_id | VARCHAR(50) | NOT NULL, UNIQUE | 회원 로그인용 ID |
| 3 | 비밀번호   | password | VARCHAR(255) | NOT NULL | 암호화하여 저장 |
| 4 | 이메일    | email | VARCHAR(100) | NOT NULL, UNIQUE | 회원 인증 및 소통용 이메일 |
| 5 | 닉네임    | nickname | VARCHAR(50) | NOT NULL, UNIQUE | 서비스 내 표시 이름 |
| 6 | 가입일    | created_at | DATETIME | NOT NULL | 회원 가입 시점 |
| 7 | 수정일    | updated_at | DATETIME | NOT NULL | 데이터 수정 시점 |
| 8 | 탈퇴일    | deleted_at | DATETIME | NULL | 회원 탈퇴 시점 |

### 제약 조건

- PK: `member_id`
- UNIQUE: `login_id`
- UNIQUE: `email`
- UNIQUE: `nickname`

---

## 3.2 study

### 테이블 정보

| 항목 | 내용 |
|------|------|
| 테이블 한글명 | 스터디 |
| 테이블 영문명 | study |
| 테이블 설명 | 스터디 모집 게시글 정보를 저장 |

### 컬럼 정의

| No. | 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 비고 |
|---|---|---|---|---|---|
| 1 | 스터디 ID | study_id | BIGINT | PK, AUTO_INCREMENT | 스터디의 고유 식별자 |
| 2 | 회원 ID | member_id | BIGINT | FK, NOT NULL | 스터디 작성자 |
| 3 | 제목 | study_title | VARCHAR(255) | NOT NULL | 스터디 모집글 제목 |
| 4 | 내용 | study_content | TEXT | NULL | 스터디 상세 설명 |
| 5 | 진행 방식 | method | VARCHAR(20) | NOT NULL | `ONLINE`, `OFFLINE` |
| 6 | 지역 | region | VARCHAR(50) | NULL | 오프라인 스터디 지역 |
| 7 | 모집 정원 | capacity | INT | NOT NULL | 최대 모집 인원 |
| 8 | 모집 상태 | study_status | VARCHAR(20) | NOT NULL | `OPEN`, `CLOSED` |
| 9 | 등록일 | created_at | DATETIME | NOT NULL | 게시글 작성 시점 |
| 10 | 수정일 | updated_at | DATETIME | NOT NULL | 게시글 수정 시점 |
| 11 | 삭제일 | deleted_at | DATETIME | NULL | 게시글 삭제 시점 |

### 제약 조건

- PK: `study_id`
- FK: `member_id` → `member(member_id)`

### 인덱스

| 인덱스명 | 컬럼 | 비고 |
|------|------|------|
| idx_study_title | study_title | 제목 검색 성능 향상 |

### 설계 메모

- `method = OFFLINE`인 경우 `region` 값이 필요할 수 있다.
- `capacity`는 1 이상이어야 한다.
- 모집 상태는 서비스 정책에 따라 관리한다.

---

## 3.3 application

### 테이블 정보

| 항목 | 내용 |
|------|------|
| 테이블 한글명 | 신청 |
| 테이블 영문명 | application |
| 테이블 설명 | 회원의 스터디 신청 정보를 저장 |

### 컬럼 정의

| No. | 컬럼 한글명 | 컬럼 영문명 | 데이터 타입 | 제약 조건 | 비고 |
|---|---|---|---|---|---|
| 1 | 신청 ID | application_id | BIGINT | PK, AUTO_INCREMENT | 신청의 고유 식별자 |
| 2 | 회원 ID | member_id | BIGINT | FK, NOT NULL | 스터디 신청 회원 |
| 3 | 스터디 ID | study_id | BIGINT | FK, NOT NULL | 신청 대상 스터디 |
| 4 | 메시지 | message | VARCHAR(255) | NULL | 신청 시 작성 메시지 |
| 5 | 신청 상태 | application_status | VARCHAR(20) | NOT NULL | `PENDING`, `APPROVED`, `REJECTED`, `CANCELED` |
| 6 | 작성일 | created_at | DATETIME | NOT NULL | 신청 시점 |
| 7 | 수정일 | updated_at | DATETIME | NOT NULL | 신청 정보 수정 시점 |

### 제약 조건

- PK: `application_id`
- FK: `member_id` → `member(member_id)`
- FK: `study_id` → `study(study_id)`

### 인덱스

| 인덱스명 | 컬럼 | 비고 |
|------|------|------|
| idx_application_status_created_at | application_status, created_at | 상태/기간별 조회 성능 향상 |

### 설계 메모

- 동일 회원은 동일 스터디에 중복 신청할 수 없다.
- 신청 취소는 삭제가 아니라 `application_status = CANCELED`로 관리한다.
- 정원 초과 방지는 승인 처리 로직에서 추가 검증이 필요하다.

---

## 3.4 comment

### 테이블 정보

| 항목 | 내용             |
|------|----------------|
| 테이블 한글명 | 댓글             |
| 테이블 영문명 | comment        |
| 테이블 설명 | 스터디에 달린 댓글을 저장 |

### 컬럼 정의

| No. | 컬럼 한글명 | 컬럼 영문명          | 데이터 타입 | 제약 조건 | 비고         |
|---|--------|-----------------|---|---|------------|
| 1 | 댓글 ID  | comment_id      | BIGINT | PK, AUTO_INCREMENT | 댓글의 고유 식별자 |
| 2 | 스터디 ID | study_id        | BIGINT | FK, NOT NULL | 댓글이 달린 스터디 |
| 3 | 회원 ID  | member_id       | BIGINT | FK, NOT NULL | 댓글 작성 회원   |
| 4 | 내용     | comment_content | TEXT | NULL | 댓글 내용      |
| 5 | 작성일    | created_at      | DATETIME | NOT NULL | 댓글 작성 시점   |
| 6 | 수정일    | updated_at      | DATETIME | NOT NULL | 댓글 수정 시점   |
| 7 | 삭제일    | deleted_at      | DATETIME | NULL | 댓글 삭제 시점   |

### 제약 조건

- PK: `comment_id`
- FK: `study_id` → `study(study_id)`
- FK: `member_id` → `member(member_id)`

### 인덱스

| 인덱스명                 | 컬럼 | 비고               |
|----------------------|------|------------------|
| idx_comment_study_id | post_id | 스터디별 댓글 조회 성능 향상 |

---

# 4. 관계 요약

| 부모 테이블 | 자식 테이블 | 관계 | 설명                    |
|--------|------|------|-----------------------|
| member | study | 1:N | 회원은 여러 스터디를 개설할 수 있다  |
| member | application | 1:N | 회원은 여러 스터디에 신청할 수 있다  |
| study  | application | 1:N | 하나의 스터디에는 여러 신청이 존재한다 |
| member | comment | 1:N | 회원은 여러 댓글을 작성할 수 있다   |
| study  | comment | 1:N | 하나의 스터디에는 여러 댓글이 존재한다 |

---

# 5. 상태값 정의

## 5.1 Study Status

| 상태값 | 설명 |
|------|------|
| OPEN | 모집 중 |
| CLOSED | 모집 마감 |

## 5.2 Application Status

| 상태값 | 설명 |
|------|------|
| PENDING | 신청 대기 |
| APPROVED | 승인 |
| REJECTED | 거절 |
| CANCELED | 신청 취소 |

---

# 6. 비즈니스 규칙

- 회원은 회원가입 후 로그인할 수 있다.
- 회원은 여러 개의 스터디를 개설할 수 있다.
- 회원은 동일한 스터디에 중복 신청할 수 없다.
- 모집 상태가 `CLOSED`인 스터디에는 신청할 수 없다.
- 스터디 신청은 `PENDING`, `APPROVED`, `REJECTED`, `CANCELED` 상태로 관리한다.
- 스터디 정원을 초과하여 승인할 수 없다.
- 댓글은 스터디에 종속된다.
- 삭제는 `deleted_at`을 이용한 소프트 삭제를 기본으로 한다.

---

# 7. ERD

물리적 모델링 기반 ERD

![ERD](erd/ERD_v3.png)

---

# 8. 참고 DDL

실제 테이블 생성 SQL은 별도 파일에서 관리한다.

- `database/ddl.sql`