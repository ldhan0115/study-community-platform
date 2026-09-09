# 📚 Study Community Platform

> 스터디 모집부터 참가 신청, 승인·거절, 댓글 관리까지 제공하는 Spring MVC 기반 커뮤니티 웹 애플리케이션

회원은 스터디를 개설하거나 다른 회원의 스터디에 참가를 신청할 수 있으며, 스터디 방장은 신청자를 승인하거나 거절하고 모집 상태를 관리할 수 있습니다.

단순 CRUD 구현에 그치지 않고, **서버 측 권한 검증**, **신청 상태 전이**, **안전한 세션 데이터 관리**, **논리적 삭제 정책**을 서비스와 도메인 계층에서 보장하는 데 집중했습니다.

| 구분     | 내용                             |
| ------ | ------------------------------ |
| 개발 기간  | 2026.02.13 ~ 2026.09.07        |
| 개발 인원  | 1명                             |
| 개발 형태  | 개인 프로젝트                        |
| 주요 관심사 | 인증 세션, 인가, 도메인 상태 관리, JPA, 테스트 |

## ⭐ 핵심 구현

### 1. 세션과 도메인 엔티티 분리

* 세션에 `Member` 엔티티 전체를 저장하지 않고 `LoginMemberSession(id, nickname)`만 저장합니다.
* 비밀번호 등 불필요한 개인정보가 세션에 포함되지 않도록 했습니다.
* 회원정보 수정 화면에서는 회원 ID를 이용해 DB의 최신 정보를 다시 조회합니다.
* 회원정보 수정 후에도 기존 회원 ID가 유지되도록 테스트했습니다.

### 2. Service 계층에서 권한 검증

* 스터디 수정·삭제는 `StudyService`가 실제 스터디 작성자를 검증합니다.
* 신청 승인·거절은 `ApplicationService`가 `Application`과 연결된 실제 스터디 작성자를 검증합니다.
* 신청 취소는 실제 신청자 본인인지 검증합니다.
* 클라이언트가 전달한 `studyId`를 조작해 다른 스터디의 신청을 변경할 수 없도록 했습니다.

### 3. 참가 신청 상태 전이 규칙

* 새 신청은 항상 `PENDING`으로 생성됩니다.
* `PENDING` 상태에서만 `APPROVED`, `REJECTED`, `CANCELED`로 변경할 수 있습니다.
* 처리가 끝난 신청의 상태를 다시 변경하면 예외가 발생합니다.
* 허용되는 전이와 금지되는 전이 조합을 도메인 테스트로 검증했습니다.

### 4. 회원·스터디·댓글 논리적 삭제

* 데이터를 바로 삭제하지 않고 `deletedAt`을 기록해 기존 연관관계와 이력을 보존합니다.
* 탈퇴 회원은 활성 회원 조회 대상과 로그인 대상에서 제외합니다.
* 탈퇴한 계정의 로그인 ID, 이메일, 닉네임은 재사용하지 않는 정책을 적용했습니다.
* 삭제된 스터디와 댓글은 일반 조회 결과에서 제외합니다.

### 5. 비밀번호 단방향 해시

* 회원가입과 회원정보 수정 시 `PasswordEncoder`로 비밀번호를 단방향 해시 처리합니다.
* 로그인 시 평문 비밀번호를 저장된 해시값과 `matches()`로 비교합니다.
* 세션에는 비밀번호를 저장하지 않습니다.

## 🖥 주요 화면

### 스터디 목록

진행 방식, 지역, 모집 상태와 모집 정원을 기준으로 등록된 스터디를 확인할 수 있습니다.

![스터디 목록 화면](./docs/images/study-list.png)

### 스터디 상세 및 참가 신청

스터디의 상세 정보와 모집 상태를 확인하고 참가를 신청할 수 있습니다.

![스터디 상세 화면](./docs/images/study-detail.png)

### 댓글 작성 및 관리

로그인 회원은 댓글을 작성할 수 있으며, 작성자 본인만 자신의 댓글을 수정하거나 삭제할 수 있습니다.

![스터디 댓글 화면](./docs/images/study-comments.png)

### 참가 신청 관리

스터디 방장은 신청 목록을 확인하고 대기 중인 신청을 승인하거나 거절할 수 있습니다.

![참가 신청 관리 화면](./docs/images/study-applications.png)

### 내 신청 내역

로그인 회원은 자신의 신청 내역과 현재 상태를 확인하고, 대기 중인 신청을 취소할 수 있습니다.

![내 신청 내역 화면](./docs/images/my-applications.png)

## 🚀 주요 기능

### 회원

* 회원가입 및 로그인
* 로그인 ID, 이메일, 닉네임 중복 검증
* 회원정보 조회 및 수정
* `PasswordEncoder`를 이용한 비밀번호 단방향 해시
* 최소 정보만 포함한 로그인 세션 DTO
* 회원 탈퇴와 논리적 삭제
* 탈퇴 회원 재로그인 차단

### 스터디

* 스터디 등록, 조회, 수정 및 삭제
* 온라인·오프라인 진행 방식과 지역 설정
* 모집 정원 및 모집 상태 관리
* 작성자 본인만 수정·삭제 가능
* 승인 인원이 정원에 도달하면 `CLOSED`로 자동 전환
* 삭제된 스터디 이력 보존을 위한 논리적 삭제

### 참가 신청

* 스터디 참가 신청
* 신청 취소 후 재신청
* 스터디 방장의 신청 승인·거절
* 신청자 본인의 신청 취소
* 유효한 신청 상태를 기준으로 중복 신청 방지
* 승인 인원을 기준으로 모집 정원 검증
* `PENDING`에서만 상태를 변경할 수 있는 상태 전이 규칙

### 댓글

* 스터디별 댓글 등록 및 조회
* 작성자 본인의 댓글 수정·삭제
* 삭제된 댓글 이력 보존을 위한 논리적 삭제

## 🔐 인증 및 권한 처리

현재 프로젝트는 Spring MVC 세션 기반 인증을 사용합니다. 화면에서 버튼을 숨기는 것에만 의존하지 않고, 데이터 변경 직전에 서버가 로그인 회원과 실제 리소스 소유자를 비교합니다.

| 영역        | 처리 방식                                         |
| --------- | --------------------------------------------- |
| 로그인 확인    | `HandlerInterceptor`로 보호 URL 접근 제어            |
| 로그인 회원 주입 | `HandlerMethodArgumentResolver`와 `@Login` 사용  |
| 세션 데이터    | `LoginMemberSession`에 회원 ID와 닉네임만 저장          |
| 스터디 수정·삭제 | 실제 스터디 작성자를 `StudyService`에서 검증               |
| 신청 승인·거절  | 실제 신청과 연결된 스터디 작성자를 `ApplicationService`에서 검증 |
| 신청 취소     | 실제 신청자 본인을 `ApplicationService`에서 검증          |
| 댓글 수정·삭제  | 실제 댓글 작성자를 `CommentService`에서 검증              |
| 탈퇴 회원     | `deletedAt IS NULL`인 활성 회원만 로그인 허용            |

## 🧩 주요 도메인 규칙

### 스터디 상태

| 상태       | 설명                          |
| -------- | --------------------------- |
| `OPEN`   | 참가 신청 및 승인이 가능한 모집 중 상태     |
| `CLOSED` | 모집이 마감되어 새로운 신청을 받을 수 없는 상태 |

### 참가 신청 상태

| 현재 상태      | 변경 가능한 상태  | 설명            |
| ---------- | ---------- | ------------- |
| `PENDING`  | `APPROVED` | 스터디 방장이 신청 승인 |
| `PENDING`  | `REJECTED` | 스터디 방장이 신청 거절 |
| `PENDING`  | `CANCELED` | 신청자가 직접 신청 취소 |
| `APPROVED` | 없음         | 처리가 끝난 최종 상태  |
| `REJECTED` | 없음         | 처리가 끝난 최종 상태  |
| `CANCELED` | 없음         | 처리가 끝난 최종 상태  |

상태값을 Controller에서 직접 변경하지 않고 `Application.approve()`, `reject()`, `cancel()`을 통해서만 변경합니다.

## 🛠 기술 스택

| 구분              | 기술                                         |
| --------------- | ------------------------------------------ |
| Language        | Java 21                                    |
| Framework       | Spring Boot 4.0.3, Spring MVC              |
| Data Access     | Spring Data JPA, Hibernate                 |
| Database        | H2                                         |
| View            | Thymeleaf, Bootstrap 5                     |
| Authentication  | HttpSession, Interceptor, ArgumentResolver |
| Password        | Spring Security Crypto, PasswordEncoder    |
| Test            | JUnit 5, AssertJ, Spring Boot Test         |
| Build           | Gradle                                     |
| Version Control | Git, GitHub                                |

## 🧪 테스트

서비스와 도메인 계층을 중심으로 정상 흐름뿐 아니라 권한 우회와 잘못된 상태 변경 같은 실패 흐름을 함께 검증합니다.

* 회원가입 중복 검증과 비밀번호 해시 저장
* 정상 회원 로그인과 탈퇴 회원 로그인 차단
* 회원정보 수정 후 회원 ID 유지 및 세션 비밀번호 미포함
* 타인의 스터디 수정·삭제 차단
* 타인의 신청 승인·거절·취소 차단
* 권한 검증 실패 후 기존 데이터와 신청 상태 유지
* 신청의 허용·금지 상태 전이 조합
* 모집 정원 초과 방지와 정원 도달 시 자동 마감
* 취소·거절 후 재신청
* 타인의 댓글 수정·삭제 차단

### 전체 테스트 실행

macOS 또는 Linux:

```bash
./gradlew clean test
```

Windows:

```powershell
.\gradlew.bat clean test
```

테스트는 `src/test/resources/application-test.yml`의 독립적인 인메모리 H2 데이터베이스를 사용합니다.

## 🔥 트러블슈팅 및 기술적 의사결정

기능 구현 과정에서 발생한 문제의 원인과 해결 과정, 적용한 테스트를 별도의 문서로 정리했습니다.

| 문제                                                                                  | 해결 과정 및 결과                                                          |
| ----------------------------------------------------------------------------------- | ------------------------------------------------------------------- |
| **[비밀번호 평문 저장](./docs/troubleshooting/password-encoding.md)**                       | `PasswordEncoder`를 적용해 저장 시 해시 처리하고 로그인 시 `matches()`로 검증했습니다.      |
| **[회원 수정 후 세션 ID 유실](./docs/troubleshooting/member-session-id-loss.md)**            | 비영속 `Member` 대신 최소 정보만 가진 세션 DTO를 저장하고 영속 엔티티의 수정 결과로 세션을 갱신했습니다.   |
| **[비로그인 사용자 NPE](./docs/troubleshooting/npe-guest-access.md)**                      | 세션이 항상 존재한다는 가정을 제거하고 비로그인 상태를 명시적으로 처리했습니다.                        |
| **[스터디 수정·삭제 권한 우회](./docs/troubleshooting/study-authorization-bypass.md)**         | 작성자 검증을 Controller에서 `StudyService`로 이동해 호출 경로와 관계없이 권한 규칙을 적용했습니다. |
| **[신청 승인·거절·취소 권한 우회](./docs/troubleshooting/prevent-api-authorization-bypass.md)** | 클라이언트가 전달한 ID 대신 `Application`에 연결된 실제 스터디와 신청자를 기준으로 권한을 검증했습니다.   |
| **[신청 상태 재변경](./docs/troubleshooting/application-state-transition.md)**             | `PENDING` 상태에서만 상태 변경을 허용하고 모든 최종 상태 사이의 재변경을 차단했습니다.               |
| **[탈퇴 회원 재로그인](./docs/troubleshooting/withdrawn-member-login.md)**                  | 활성 회원 전용 조회 조건을 적용해 `deletedAt`이 기록된 회원의 로그인을 차단했습니다.               |
| **[신청 취소 후 재신청](./docs/troubleshooting/reapply-after-cancel-bug.md)**               | `PENDING`, `APPROVED` 상태만 유효한 중복 신청으로 판단하도록 정책을 변경했습니다.             |
| **[회원 수정 시 본인 정보 중복 판정](./docs/troubleshooting/self-data-validation-bug.md)**       | 수정 대상 회원의 현재 값과 실제 변경 값을 비교한 후 필요한 항목만 중복 검사했습니다.                   |
| **[TestDataInit 초기화 시점](./docs/troubleshooting/test-data-init-failed.md)**          | 애플리케이션 준비 이벤트 이후 트랜잭션 안에서 샘플 데이터를 저장하도록 변경했습니다.                     |
| **[Member createdAt null 예외](./docs/troubleshooting/member-created-at-null.md)**    | `@PrePersist`, `@PreUpdate`로 생성일과 수정일을 자동 관리했습니다.                   |

## 📂 프로젝트 구조

```text
src
├── main
│   ├── java/com/study/study_community_platform
│   │   ├── config
│   │   ├── controller
│   │   │   └── web
│   │   │       ├── argumentresolver
│   │   │       ├── interceptor
│   │   │       ├── member
│   │   │       ├── session
│   │   │       └── study
│   │   ├── domain
│   │   ├── repository
│   │   └── service
│   │       └── dto
│   └── resources
│       ├── static/css
│       ├── templates
│       └── application.yml
└── test
    ├── java/com/study/study_community_platform
    │   ├── controller
    │   ├── domain
    │   └── service
    └── resources/application-test.yml
```

## 📊 ERD 및 설계 문서

회원, 스터디, 참가 신청, 댓글의 연관관계를 기준으로 데이터 모델을 구성했습니다.

![Study Community Platform ERD](./docs/erd/ERD_v3.png)

* [요구사항](./docs/01-requirements.md)
* [개념적 데이터 모델](./docs/02-conceptual-model.md)
* [논리적 데이터 모델](./docs/03-logical-model.md)
* [물리적 데이터 모델](./docs/04-physical-model.md)

## ▶️ 로컬 실행 방법

### 요구 환경

* Java 21
* H2 Database

### 저장소 복제

```bash
git clone https://github.com/ldhan0115/study-community-platform.git
cd study-community-platform
```

### H2 실행

애플리케이션은 다음 H2 TCP 데이터베이스를 사용합니다.

```text
jdbc:h2:tcp://localhost/~/study-platform
```

먼저 H2 TCP Server를 실행한 다음 애플리케이션을 시작합니다.

macOS 또는 Linux:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

실행 후 http://localhost:8080에 접속합니다.

### 샘플 계정

`local` 프로필에서 `TestDataInit`이 화면 확인용 데이터를 생성합니다.

| 역할       | 로그인 ID  | 비밀번호       |
| -------- | ------- | ---------- |
| 스터디 방장   | `test1` | `test1234` |
| 참가 신청 회원 | `test2` | `test1234` |

## 📌 향후 개선 계획

* 동시 승인 요청에도 모집 정원을 초과하지 않도록 동시성 제어 적용
* 도메인별 예외 타입과 전역 예외 처리 도입
* 목록 조회 페이지네이션과 검색 조건 추가
* 실행 환경별 설정 분리 및 MySQL 전환
* GitHub Actions를 이용한 테스트 자동화
* Docker 이미지와 클라우드 배포 환경 구성
* Spring Security 기반 인증·인가 구조로 확장
