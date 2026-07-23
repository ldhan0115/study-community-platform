# 논리적 모델링 (Logical Data Modeling)

개념적 모델링에서 정의된 엔티티를 기반으로 **테이블 구조와 관계를 정의하는 논리적 모델링**

논리적 모델링 단계에서는 **PK, FK, 데이터 타입, 제약조건 등을 설계**한다.

---

# 1. Member 테이블

회원 정보를 저장하는 테이블

| 컬럼 | 타입 | 제약조건 | 설명 |
|----|----|----|----|
| member_id | BIGINT | PK | 회원 식별자 |
| login_id | VARCHAR(50) | UNIQUE | 로그인 ID |
| password | VARCHAR(255) | NOT NULL | 비밀번호 |
| email | VARCHAR(100) | UNIQUE | 이메일 |
| nickname | VARCHAR(50) | UNIQUE | 닉네임 |
| created_at | DATETIME |  | 생성일 |
| updated_at | DATETIME |  | 수정일 |
| deleted_at | DATETIME | NULL | 삭제일 |

---

# 2. Study 테이블

스터디 모집 정보를 저장하는 테이블

| 컬럼 | 타입 | 제약조건 | 설명 |
|----|----|----|----|
| study_id | BIGINT | PK | 스터디 식별자 |
| member_id | BIGINT | FK | 스터디 작성자 |
| study_title | VARCHAR(255) | NOT NULL | 제목 |
| study_content | TEXT | NULL | 내용 |
| method | VARCHAR(20) | NOT NULL | 진행 방식 |
| region | VARCHAR(50) | NULL | 지역 |
| capacity | INT | NOT NULL | 모집 정원 |
| study_status | VARCHAR(20) | NOT NULL | 모집 상태 |
| created_at | DATETIME |  | 생성일 |
| updated_at | DATETIME |  | 수정일 |
| deleted_at | DATETIME | NULL | 삭제일 |

---

# 3. Application 테이블

스터디 참여 신청 정보를 저장하는 테이블

| 컬럼 | 타입 | 제약조건 | 설명 |
|----|----|----|----|
| application_id | BIGINT | PK | 신청 식별자 |
| member_id | BIGINT | FK | 신청 회원 |
| study_id | BIGINT | FK | 신청 스터디 |
| message | VARCHAR(255) | NULL | 신청 메시지 |
| application_status | VARCHAR(20) | NOT NULL | 신청 상태 |
| created_at | DATETIME |  | 생성일 |
| updated_at | DATETIME |  | 수정일 |

---

# 4. Comment 테이블

스터디 댓글을 저장하는 테이블

| 컬럼              | 타입 | 제약조건 | 설명     |
|-----------------|----|----|--------|
| comment_id      | BIGINT | PK | 댓글 식별자 |
| study_id        | BIGINT | FK | 스터디    |
| member_id       | BIGINT | FK | 작성 회원  |
| comment_content | TEXT | NULL | 댓글 내용  |
| created_at      | DATETIME |  | 작성일    |
| updated_at      | DATETIME |  | 수정일    |
| deleted_at      | DATETIME | NULL | 삭제일    |

---

# 6. ERD

논리적 모델링 기반 ERD

![ERD](erd/ERD_v2.png)




