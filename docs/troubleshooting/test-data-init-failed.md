# TestDataInit 저장 실패 예외 해결

## 문제 상황
- 초기 더미 데이터 세팅을 위해 `@PostConstruct` 내에서 `memberRepository.save()` 호출 시 에러가 발생하거나 데이터 저장이 실패함

## 원인
- `@PostConstruct` 시점에는 스프링의 AOP 기반 트랜잭션(`@Transactional`)이 아직 완전히 초기화되거나 적용되지 않음
- 트랜잭션 없이 JPA 엔티티를 저장하려 할 때 영속성 컨텍스트가 정상 작동하지 않거나, `createdAt` 자동 생성(Lifecycle Callback)이 누락되어 Not-Null 예외가 발생함

## 해결 방법
- `@PostConstruct` 대신 애플리케이션 컨텍스트가 완전히 초기화된 후 실행되는 `@EventListener(ApplicationReadyEvent.class)`를 사용함
- 해당 메서드에 `@Transactional`을 부여하여 스프링 빈이 모두 등록된 이후에 안정적인 데이터 저장 환경이 보장되도록 로직을 수정함

## 배운 점
- 스프링 컨테이너의 빈(Bean) 생성 시점과 AOP 프록시(트랜잭션)가 엮이는 라이프사이클의 순서가 다르다는 것을 깊이 이해하게 되었습니다.
- 초기화 로직에 트랜잭션 등 스프링의 핵심 부가 기능이 필요할 때는 객체 생성 직후(`@PostConstruct`)가 아닌, 컨텍스트가 완전히 준비된 이후(이벤트 리스너 활용)에 실행해야 안전하다는 스프링의 동작 원리를 체득했습니다.