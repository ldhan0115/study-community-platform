# 📚 Study Community Platform
> **"스터디 개설부터 지원, 정원 관리 및 마감까지 하나의 완벽한 사이클을 제공하는 스터디 매칭 플랫폼"**

## 💡 프로젝트 소개
단순한 게시판 형태를 넘어, 비즈니스 로직의 정합성과 예외 처리에 집중한 백엔드 API 서버입니다.
객체지향적인 도메인 설계와 JPA의 영속성 컨텍스트 생명주기를 깊이 이해하고, 실무에서 발생할 수 있는 데이터 무결성 문제 및 인가(Authorization) 보안 취약점을 선제적으로 방어하는 데 주안점을 두었습니다.

## 🛠 기술 스택
- **Language:** Java 21
- **Framework:** Spring Boot 4.x
- **Data:** Spring Data JPA, MySQL
- **Template Engine:** Thymeleaf
- **Build Tool:** Gradle

## 🚀 핵심 기능 (Core Features)
- **스터디 모집 및 정원(Capacity) 관리 시스템:**
  - 스터디 방장의 승인/거절 권한(Authorization) 로직 구현
  - 동시성 및 데이터 정합성을 고려하여 승인 시 정원 초과를 방어하는 비즈니스 로직 적용
  - 정원 충족 시 스터디 모집 상태 자동 마감(`CLOSED`) 처리
- **안전하고 유연한 데이터 관리 정책:**
  - 애플리케이션 레벨의 중복 신청 검증 로직 구현 (과거 취소 이력이 있어도 재신청 가능하도록 DB 복합 유니크 제약조건 제거 및 구조 개선)
  - 외래키(FK) 참조 무결성 유지를 위해 스터디 및 댓글 도메인에 논리적 삭제(Soft Delete / `@SQLRestriction`) 일괄 적용
- **보안 및 예외 방어:**
  - 비로그인 사용자의 비정상적 URL 접근 시 발생하는 `NullPointerException(NPE)` 방어 로직 구축
  - 엔티티 생명주기(Lifecycle Callback)를 활용한 `createdAt` 누락(Not-Null Constraint) 에러 방지

## 🔥 트러블슈팅 및 기술적 의사결정
> 기능 구현에 그치지 않고, 프레임워크의 동작 원리를 파악하여 논리적 버그와 잠재적 장애 요소를 해결한 과정입니다. (제목 클릭 시 상세 문서로 이동)

| 이슈 및 고민                                                                                                    | 해결 방안 및 배운 점                                                                                                                                              |
|:-----------------------------------------------------------------------------------------------------------|:----------------------------------------------------------------------------------------------------------------------------------------------------------|
| **[비로그인 사용자 NPE(NullPointerException) 방어](./docs/troubleshooting/npe-guest-access.md)**                    | 세션 데이터 의존 로직에 방어적 프로그래밍(Null Check)을 도입하여 예외적 URL 직접 입력 시 발생하는 서버 다운 예외 처리를 누락한 문제를 해결하고 안정성을 확보함.                                                        |
| **[API 강제 호출을 통한 권한 우회 차단](./docs/troubleshooting/prevent-api-authorization-bypass.md)**                   | 프론트엔드의 화면 숨김 처리에 의존하지 않고, Controller 계층에서 세션 ID와 리소스 소유자 ID를 직접 비교하여 불일치 시 접근을 원천 차단하는 인가(Authorization) 검증 로직을 추가함.                                      |
| **[스터디 신청 취소 후 재신청 불가 버그 해결](./docs/troubleshooting/reapply-after-cancel-bug.md)**                         | DB 단의 복합 유니크 키 제약조건을 제거하고, 상태 값(PENDING, APPROVED) 기반의 유효한 신청 건에 대해서만 중복 신청으로 간주하도록 애플리케이션 레벨의 로직으로 이관하여 비즈니스 유연성을 확보함.                                   |
| **[회원 정보 수정 시 '본인 데이터' 중복 검증 오류 해결](./docs/troubleshooting/self-data-validation-bug.md)**                  | 생성(Create)과 수정(Update) 시의 검증 맥락 차이를 분리하여 본인 데이터는 제외하고 검증하도록 방어 로직을 추가하고, Spring Data JPA의 `existsBy`를 활용하여 불필요한 엔티티 조회를 막아 쿼리 성능을 최적화함.                   |
| **[TestDataInit 빈 생성 주기와 Transactional 충돌 해결](./docs/troubleshooting/test-data-init-failed.md)** | `@PostConstruct`와 AOP 기반 프록시 트랜잭션의 초기화 시점 차이를 분석하여, 컨텍스트가 완전히 초기화된 후 실행되는 `@EventListener(ApplicationReadyEvent.class)`를 활용하여 안전한 데이터 저장 환경을 구축함.         |
| **[Member createdAt null 예외 해결](./docs/troubleshooting/member-created-at-null.md)**                        | DB 스키마 제약조건(`not-null`)에만 의존하지 않고, JPA 라이프사이클 콜백(`@PrePersist`, `@PreUpdate`)을 활용하여 엔티티 저장 및 수정 시점에 시간 컬럼이 자동으로 초기화되도록 개선함.                               |

## 📂 ERD (Entity Relationship Diagram)
- [요구사항 명세서 및 논리/물리 모델링 세부 내용 보기](./docs/04-physical-model.md)
- **ERD (Entity Relationship Diagram)**
  <img src="./docs/erd/ERD_v3.png" width="800">