# Member createdAt null 예외 해결

## 문제 상황
- `MemberService.join()` 테스트 실행 중 DB의 `createdAt` 컬럼이 `null`이라는 에러 메시지와 함께 테스트가 실패하는 예외 발생

## 원인
- `Member` 엔티티의 `createdAt`, `updatedAt` 컬럼은 `nullable = false`로 설정되어 있음
- 그러나 객체 생성 시 해당 값이 초기화되지 않은 상태로 DB 저장을 시도함
- DB의 Not-Null 제약 조건을 만족시키지 못하여 예외가 발생함

## 해결 방법
- JPA 라이프사이클 콜백(Lifecycle Callback)인 `@PrePersist`와 `@PreUpdate`를 사용하여 엔티티가 저장 및 수정되는 시점에 시간 컬럼이 자동으로 초기화되도록 수정함

## 배운 점
- DB 스키마에 제약조건(`not-null`)을 거는 것만으로는 데이터 무결성을 완벽히 보장할 수 없으며, 애플리케이션(Entity) 계층에서도 저장되기 전의 상태를 확실하게 제어해야 함을 배웠습니다.
- JPA 라이프사이클 콜백(`@PrePersist`, `@PreUpdate`)을 활용하면 생성일/수정일과 같은 공통 관심사(Auditing) 로직을 비즈니스 코드에서 분리하여 깔끔하게 자동화할 수 있다는 것을 깨달았습니다.