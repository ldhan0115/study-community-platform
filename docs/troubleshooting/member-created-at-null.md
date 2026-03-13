# Member createdAt null 예외 해결

## 문제 상황
- MemberService.join()` 테스트 실행 중 `createdAt` 컬럼이 `null`이라는 예외가 발생

## 원인
- `Member` 엔티티의 `createdAt`, `updatedAt` 컬럼은 `nullable = false`로 설정
- 객체 생성 시 해당 값이 초기화되지 않은 상태로 DB 저장 시도
- DB의 not-null 제약 조건을 만족시키지 못하여 예외 발생 

## 해결 방법
- JPA 라이프사이클 콜백을 사용하여 엔티티 저장 및 수정 시점에
시간 컬럼이 자동으로 초기화되도록 수정
