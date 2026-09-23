# 📚 Study Community Platform

> 스터디 모집부터 참가 신청, 승인·거절, 댓글 관리까지 제공하는 Spring MVC 기반 커뮤니티 웹 애플리케이션

회원은 스터디를 개설하거나 참가를 신청할 수 있으며, 스터디 방장은 신청자를 승인하거나 거절할 수 있습니다. 승인된 신청 수가 모집 정원에 도달하면 스터디가 자동으로 마감됩니다.

**Spring Security 기반 세션 인증**, **서비스 계층의 권한 검증**, **신청 상태 전이**, **논리적 삭제**, **도메인 예외 처리**를 구현했습니다. 정상 요청과 권한 위반·잘못된 상태 변경을 확인하는 테스트도 작성하고 있습니다.

| 구분 | 내용 |
| --- | --- |
| 개발 기간 | 2026.02.13 ~ 진행 중 |
| 개발 인원 | 1명 |
| 개발 형태 | 개인 프로젝트 |
| 주요 관심사 | 인증·인가, 도메인 상태 관리, 데이터 정합성, JPA, 테스트 |

## ⭐ 핵심 구현

### 1. Spring Security 기반 세션 인증

- `SecurityFilterChain`에서 공개 URL과 로그인이 필요한 URL을 구분합니다.
- 로그인 POST 요청은 Spring Security의 인증 필터가 처리합니다.
- `CustomUserDetailsService`에서 탈퇴하지 않은 회원을 조회하고, `LoginMemberPrincipal`로 인증에 필요한 정보를 전달합니다.
- 회원가입·회원정보 수정 시 `PasswordEncoder`로 비밀번호를 단방향 해시 처리합니다.
- 로그인 시 Spring Security가 저장된 해시와 입력 비밀번호를 검증합니다.
- CSRF 검증을 활성화하고, 로그인 성공 시 기존 세션 ID를 변경하도록 설정했습니다.
- 로그아웃 시 세션과 인증 정보를 제거하고 `JSESSIONID` 쿠키를 삭제하도록 설정했습니다.

### 2. 화면용 세션 데이터와 도메인 엔티티 분리

- 화면용 `loginMember` 세션 속성에는 `Member` 엔티티 대신 `LoginMemberSession(id, nickname)`을 저장합니다.
- 해당 DTO에는 비밀번호와 이메일을 포함하지 않습니다.
- Spring Security의 인증 정보는 `SecurityContext`에서 별도로 관리합니다.
- `@Login`과 `LoginMemberArgumentResolver`로 컨트롤러에 로그인 회원 정보를 전달합니다.
- 회원정보 수정 화면에서는 회원 ID로 DB의 최신 정보를 조회하며, 수정 후에는 화면용 세션 DTO를 갱신합니다.

### 3. 서비스 계층의 리소스 소유자 검증

- 스터디 수정·삭제는 `StudyService`에서 실제 작성자를 확인합니다.
- 신청 승인·거절은 `ApplicationService`에서 신청과 연결된 실제 스터디의 작성자를 확인합니다.
- 신청 취소는 실제 신청자 본인만 수행할 수 있습니다.
- 댓글 수정·삭제는 `CommentService`에서 실제 댓글 작성자를 확인합니다.
- 신청 승인·거절의 권한 판단에는 클라이언트가 별도로 전달한 `studyId` 대신 서버에서 조회한 연관관계를 사용합니다.

### 4. 참가 신청 상태 전이와 정원 검사

- 새 신청은 `PENDING` 상태로 생성됩니다.
- `PENDING`에서만 `APPROVED`, `REJECTED`, `CANCELED`로 변경할 수 있습니다.
- 처리된 신청의 상태를 다시 변경하면 `BusinessRuleException`이 발생합니다.
- 동일 회원·스터디에 `PENDING` 또는 `APPROVED` 신청이 있으면 추가 신청을 거부합니다.
- 취소·거절 이력은 보존하며, 유효한 신청이 없고 모집 중이면 재신청할 수 있습니다.
- 승인 시 현재 승인된 신청 수와 정원을 비교하며, 승인 후 정원에 도달하면 `CLOSED`로 전환합니다.

### 5. 논리적 삭제와 조회 정책

- 회원·스터디·댓글을 즉시 삭제하지 않고 `deletedAt`을 기록합니다.
- 로그인 및 활성 회원 단건 조회는 `deletedAt IS NULL` 조건을 적용합니다.
- 탈퇴한 회원의 로그인 ID·이메일·닉네임은 재사용하지 않는 정책을 적용했습니다.
- 스터디와 댓글은 `@SQLRestriction`으로 삭제된 데이터를 일반 조회에서 제외합니다.
- 내 신청 내역은 스터디를 fetch join으로 함께 조회하고, 삭제된 스터디의 신청을 제외합니다.

### 6. 도메인 예외와 공통 오류 화면

`GlobalExceptionHandler`에서 도메인 예외를 분류하고, `ModelAndView`에 실제 HTTP 상태와 오류 화면 데이터를 설정합니다.

| 예외 | 의미 | 전역 처리 시 HTTP 상태 |
| --- | --- | --- |
| `ResourceNotFoundException` | 요청한 리소스를 찾을 수 없음 | `404 Not Found` |
| `ForbiddenOperationException` | 해당 작업을 수행할 권한이 없음 | `403 Forbidden` |
| `BusinessRuleException` | 현재 비즈니스 규칙상 처리할 수 없음 | `400 Bad Request` |

컨트롤러에서 별도로 처리하지 않은 위 예외는 `error/error.html`로 표시합니다. 참가 신청 과정의 일부 비즈니스 오류는 컨트롤러에서 처리해 상세 화면에 안내 메시지를 보여줍니다.

## 🖥 주요 화면

### 스터디 목록

등록된 스터디의 제목, 소개, 진행 방식, 지역, 작성자와 모집 정원을 확인할 수 있습니다.

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

로그인 회원은 자신의 신청 내역과 상태를 확인하고 대기 중인 신청을 취소할 수 있습니다. 삭제된 스터디의 신청은 목록에서 제외합니다.

![내 신청 내역 화면](./docs/images/my-applications.png)

## 🚀 주요 기능

| 영역 | 기능 |
| --- | --- |
| 회원 | 회원가입, 로그인·로그아웃, 정보 수정, 탈퇴, 로그인 ID·이메일·닉네임 중복 검사 |
| 인증 | Spring Security 세션 인증, 탈퇴 회원 재로그인 차단, CSRF 검증, 로그인 시 세션 ID 변경 |
| 스터디 | 등록·목록·상세·수정·삭제, 온라인·오프라인 구분, 오프라인 지역 입력, 정원 설정 |
| 참가 신청 | 신청·승인·거절·취소, 유효 상태 기준 중복 검사, 취소·거절 후 재신청, 정원 도달 시 자동 마감 |
| 댓글 | 스터디별 댓글 조회·등록, 작성자 본인의 수정·삭제 |
| 오류 처리 | 도메인 예외 분류, HTTP 400·403·404 응답과 공통 오류 화면 |

## 🔐 인증 및 권한 처리

URL 접근 제어는 Spring Security가 담당하고, 리소스 소유자 검증은 서비스 계층에서 수행합니다.

| 영역 | 처리 방식 |
| --- | --- |
| 로그인 처리 | Spring Security 인증 필터가 `POST /members/login` 처리 |
| 회원 조회 | `CustomUserDetailsService`에서 활성 회원 조회 |
| 인증 사용자 | `LoginMemberPrincipal`에 회원 ID·닉네임과 인증에 필요한 정보 구성 |
| 보호 URL 접근 | `SecurityFilterChain`의 요청별 접근 정책 적용 |
| 로그인 회원 주입 | `@Login`과 `LoginMemberArgumentResolver` 사용 |
| 화면용 세션 데이터 | `LoginMemberSession`에 회원 ID·닉네임 저장 |
| 비밀번호 검증 | `PasswordEncoder`를 사용하는 Spring Security 인증 처리 |
| CSRF | 상태를 변경하는 요청에 CSRF 토큰 검증 적용 |
| 세션 보호 | 로그인 성공 시 `changeSessionId()` 적용 |
| 로그인 후 이동 | 저장된 보호 페이지 요청이 있으면 복귀하고, 없으면 `/`로 이동 |
| 로그아웃 | 세션 무효화, 인증 정보 정리, 세션 쿠키 삭제 |
| 스터디 수정·삭제 | `StudyService`에서 실제 작성자 검증 |
| 신청 승인·거절·취소 | `ApplicationService`에서 실제 스터디 작성자 또는 신청자 검증 |
| 댓글 수정·삭제 | `CommentService`에서 실제 작성자 검증 |

### 공개 페이지와 보호 페이지

| 구분 | 주요 경로 |
| --- | --- |
| 비로그인 조회 가능 | `/`, `/members/join`, `/members/login`, `/studies`, 숫자 ID의 `/studies/{studyId}` |
| 로그인 필요 | 회원정보 수정, 스터디 개설·수정·삭제, 신청자 관리, 내 신청 내역, 신청 및 댓글 변경 요청 |

회원가입과 로그인 POST 요청도 인증 없이 접근할 수 있지만 CSRF 검증은 적용됩니다. 정적 파일과 오류 경로는 별도로 공개합니다.

로그인 후 이동 주소를 결정할 때 임의의 `redirectURL` 요청 파라미터는 사용하지 않습니다.

## 🧩 주요 도메인 규칙

### 스터디 상태

| 상태 | 의미 |
| --- | --- |
| `OPEN` | 새로운 참가 신청을 받을 수 있는 상태 |
| `CLOSED` | 모집이 마감되어 새로운 참가 신청을 받지 않는 상태 |

승인 시 `APPROVED` 상태의 신청 수를 기준으로 정원을 검사합니다. 승인 후 정원에 도달하면 자동으로 마감합니다.

### 참가 신청 상태

| 현재 상태 | 변경 가능한 상태 | 수행 주체 |
| --- | --- | --- |
| `PENDING` | `APPROVED` | 스터디 작성자 |
| `PENDING` | `REJECTED` | 스터디 작성자 |
| `PENDING` | `CANCELED` | 신청자 본인 |
| `APPROVED` | 없음 | 최종 상태 |
| `REJECTED` | 없음 | 최종 상태 |
| `CANCELED` | 없음 | 최종 상태 |

상태는 `Application.approve()`, `reject()`, `cancel()`을 통해 변경합니다. 취소·거절 후 재신청은 기존 신청의 상태를 되돌리지 않고 새로운 신청을 생성합니다.

현재 정원과 중복 신청 검사는 순차 요청을 기준으로 구현되어 있습니다. 동시 요청 제어와 승인 인원보다 작은 정원으로의 수정 방어는 추가 개선 대상입니다.

## 🛠 기술 스택

| 구분 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.3, Spring MVC |
| Security | Spring Security, 세션 인증, PasswordEncoder |
| Data Access | Spring Data JPA, Hibernate |
| Database | H2 |
| View | Thymeleaf, Bootstrap 5 |
| Validation | Jakarta Bean Validation |
| Test | JUnit Jupiter, AssertJ, MockMvc, Spring Security Test |
| Build | Gradle Wrapper 9.3.1 |
| Version Control | Git, GitHub |

## 🧪 테스트

현재 작성된 테스트는 인증 접근 정책, 서비스 권한, 신청 상태 전이와 주요 CRUD 흐름을 확인합니다.

| 영역 | 작성된 테스트 범위 |
| --- | --- |
| 회원 | 가입 정보 중복 검사, 비밀번호 해시 검증, 수정 후 회원 ID 유지, 탈퇴 회원 조회 제외, 탈퇴한 로그인 ID 재사용 차단 |
| 인증·접근 | 비로그인 공개 페이지 조회, 보호 페이지 로그인 이동, 로그인 후 보호 페이지 접근 |
| 로그인 보안 | 정상 로그인, 탈퇴 회원 로그인 실패, CSRF 토큰 없는 로그인 차단, 임의 외부 redirect 파라미터 무시, 로그인 전후 세션 ID 변경 |
| 화면용 세션 | 회원 수정 후 ID·닉네임 갱신, `LoginMemberSession`에 비밀번호 필드가 없음을 확인 |
| 스터디 | 생성·조회·수정·삭제·마감, 제목·최소 정원 검사, 타인의 수정 화면 접근·수정·삭제 차단 |
| 참가 신청 | 신청·조회·취소, 중복 신청 거부, 마감 스터디 신청 거부, 순차 승인 시 정원 초과 거부, 타인의 승인·거절·취소 차단 |
| 신청 상태 | 초기 `PENDING`, 허용되는 전이 3개와 최종 상태 간 금지되는 전이 9개, 승인 후 거절 차단 |
| 논리 삭제 | 삭제된 스터디의 신청이 내 신청 목록에서 제외됨을 확인 |
| 댓글 | 등록·조회·수정·삭제의 정상 흐름 |
| 전역 예외 처리 | 테스트용 컨트롤러에서 발생시킨 도메인 예외의 400·403·404 상태, 오류 뷰와 모델 확인 |

회원정보 수정의 기존 컨트롤러 테스트는 메서드를 직접 호출하며, 인증 테스트는 MockMvc로 요청을 수행합니다. 취소·거절 후 재신청, 타인의 댓글 변경 차단, 정원 도달에 따른 자동 마감 상태, 동시 요청에 대한 전용 회귀 테스트는 보강할 예정입니다.

### 전체 테스트 실행

macOS 또는 Linux:

```bash
./gradlew clean test
```

Windows:

```powershell
.\gradlew.bat clean test
```

Spring Boot 통합 테스트는 `test` 프로필과 `src/test/resources/application-test.yml`의 인메모리 H2를 사용합니다. 테스트용 H2 TCP 서버를 별도로 실행할 필요는 없습니다.

테스트 실행 후 결과는 `build/reports/tests/test/index.html`에서 확인할 수 있습니다.

## 🔥 트러블슈팅 및 기술적 의사결정

구현 과정에서 경험한 문제와 개선 과정을 문서로 기록했습니다.

| 문제 | 개선 내용 |
| --- | --- |
| [비밀번호 평문 저장](./docs/troubleshooting/password-encoding.md) | `PasswordEncoder`로 가입·수정 시 비밀번호를 해시 처리하고 인증 시 검증하도록 개선 |
| [회원 수정 후 세션의 회원 ID 유실](./docs/troubleshooting/member-session-id-loss.md) | 수정용 DTO와 화면용 세션 DTO를 도입하고 기존 영속 회원의 수정 결과로 세션 정보 갱신 |
| [비로그인 사용자 NPE](./docs/troubleshooting/npe-guest-access.md) | 상세 조회에서 비로그인 상태를 처리해 회원 정보 접근 오류 방지 |
| [스터디 수정·삭제 권한 우회](./docs/troubleshooting/study-authorization-bypass.md) | 실제 작성자 검증을 `StudyService`로 이동 |
| [신청 승인·거절·취소 권한 우회](./docs/troubleshooting/prevent-api-authorization-bypass.md) | 신청에 연결된 실제 스터디 작성자와 신청자를 기준으로 권한 검증 |
| [신청 상태 재변경](./docs/troubleshooting/application-state-transition.md) | `PENDING`에서만 상태 변경을 허용하고 최종 상태의 재변경 차단 |
| [탈퇴 회원 재로그인](./docs/troubleshooting/withdrawn-member-login.md) | 로그인 대상 조회에 활성 회원 조건 적용 |
| [신청 취소 후 재신청](./docs/troubleshooting/reapply-after-cancel-bug.md) | `PENDING`·`APPROVED`만 유효한 중복 신청으로 판단하도록 변경 |
| [회원 수정 시 본인 정보 중복 판정](./docs/troubleshooting/self-data-validation-bug.md) | 현재 정보와 실제 변경 값을 비교해 필요한 항목만 중복 검사 |
| [TestDataInit 초기화 시점](./docs/troubleshooting/test-data-init-failed.md) | 애플리케이션 준비 이벤트 이후 트랜잭션 안에서 샘플 데이터 생성 |
| [Member createdAt null 예외](./docs/troubleshooting/member-created-at-null.md) | `@PrePersist`·`@PreUpdate`로 생성일과 수정일 관리 |

이후 인증 처리를 Spring Security로 전환하고, 도메인 예외와 전역 오류 처리기를 추가했습니다. 일부 트러블슈팅 문서에는 해당 개선 당시의 코드와 클래스명이 포함되어 있습니다.

## 📂 프로젝트 구조

Java 패키지의 기준 경로는 `src/main/java/com/study/study_community_platform`입니다.

| 경로 | 역할 |
| --- | --- |
| `config` | SecurityFilterChain, PasswordEncoder, MVC 설정 |
| `config/security` | 사용자 조회, 인증 principal, 로그인 성공 처리 |
| `controller` | 회원·스터디·신청·댓글·홈 요청 처리 |
| `controller/advice` | 도메인 예외의 전역 처리 |
| `controller/web/argumentresolver` | `@Login`과 로그인 회원 파라미터 주입 |
| `controller/web/member` | 회원 가입·수정·로그인 폼 |
| `controller/web/session` | 화면용 로그인 회원 DTO |
| `controller/web/study` | 스터디 등록·수정 폼 |
| `domain` | 엔티티와 상태·진행 방식 Enum |
| `exception` | 비즈니스 규칙·권한·리소스 조회 예외 |
| `repository` | Spring Data JPA 조회 |
| `service` | 트랜잭션, 권한 검증, 비즈니스 로직 |
| `service/dto` | 회원정보 수정 데이터 전달 |
| `TestDataInit.java` | local 프로필의 샘플 데이터 생성 |

| 저장소 경로 | 역할 |
| --- | --- |
| `src/main/resources/templates` | Thymeleaf 화면과 오류 페이지 |
| `src/main/resources/static/css` | 공통 스타일 |
| `src/main/resources/application.yml` | 현재 로컬 실행 설정 |
| `src/test/java` | 컨트롤러·인증·도메인·서비스 테스트 |
| `src/test/resources/application-test.yml` | 테스트용 인메모리 H2 설정 |
| `docs` | 요구사항·설계·ERD·화면·트러블슈팅 문서 |
| `database/ddl.sql` | MySQL을 기준으로 작성한 설계용 DDL |

## 📊 ERD 및 설계 문서

회원, 스터디, 참가 신청, 댓글의 연관관계를 기준으로 데이터 모델을 구성했습니다.

![Study Community Platform ERD](./docs/erd/ERD_v3.png)

- [요구사항](./docs/01-requirements.md)
- [개념적 데이터 모델](./docs/02-conceptual-model.md)
- [논리적 데이터 모델](./docs/03-logical-model.md)
- [물리적 데이터 모델](./docs/04-physical-model.md)

현재 애플리케이션은 H2에서 실행되며, 물리 모델과 `database/ddl.sql`은 MySQL을 기준으로 작성한 설계 자료입니다. 실제 엔티티와 설계 DDL의 일치 여부는 MySQL 전환 과정에서 정리할 예정입니다.

## ▶️ 로컬 실행 방법

### 요구 환경

- Java 21
- H2 Database TCP Server
- Gradle Wrapper 최초 실행 시 의존성을 다운로드할 수 있는 네트워크 환경

### 저장소 복제

```bash
git clone https://github.com/ldhan0115/study-community-platform.git
cd study-community-platform
```

### H2 설정

H2 TCP Server를 먼저 실행하고 다음 데이터베이스에 접속할 수 있도록 준비합니다.

| 항목 | 값 |
| --- | --- |
| JDBC URL | `jdbc:h2:tcp://localhost/~/study-platform` |
| Username | `sa` |
| Password | 비워 둠 |

현재 기본 활성 프로필은 `local`입니다. 로컬 설정의 `ddl-auto: create`는 앱 시작 시 스키마를 다시 생성하므로 기존 개발 데이터가 초기화됩니다.

### 애플리케이션 실행

macOS 또는 Linux:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

실행 후 [로컬 애플리케이션](http://localhost:8080)에 접속합니다.

### 샘플 계정

`local` 프로필의 `TestDataInit`이 화면 확인용 회원·스터디·댓글·신청 데이터를 생성합니다.

| 확인할 화면 | 로그인 ID | 비밀번호 |
| --- | --- | --- |
| 개설한 스터디의 신청자 관리 | `test1` | `test1234` |
| 참가 신청과 내 신청 내역 | `test2` | `test1234` |

## 📌 향후 개선 계획

- 댓글·신청 메시지·스터디 입력의 서버 검증과 길이 제한 보완
- 회원정보 수정 시 중복 입력 오류를 폼에 안내하도록 개선
- 정원 수정, 방장 본인 신청, 탈퇴·삭제 이후 요청의 정책 정리
- 동시 승인·중복 신청·신청 상태 변경에 대한 동시성 제어와 통합 테스트 추가
- 조회 쿼리 측정, 연관 데이터 조회 개선, 목록 페이지네이션과 검색 조건 추가
- 실행 환경별 설정 분리, MySQL 전환과 스키마 관리
- GitHub Actions를 이용한 테스트 자동화
- Docker 실행 환경과 배포 구성
- 실제 요청을 통한 권한·입력 검증 회귀 테스트 및 문서 보강