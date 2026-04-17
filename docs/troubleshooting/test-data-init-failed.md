# TestDataInit 저장 실패 예외 해결

## 문제 상황
- @PostConstruct 내에서 memberRepository.save() 호출 시 에러 발생 혹은 데이터 저장 실패

## 원인
 - @PostConstruct 시점에는 스프링의 AOP 기반 트랜잭션(@Transactional)이 아직 적용되지 않음

 - 트랜잭션 없이 JPA 엔티티를 저장하려 할 때 영속성 컨텍스트가 정상 작동하지 않거나, createdAt 자동 생성(Lifecycle Callback)이 누락되어 Not-Null 예외 발생

## 해결 방법
 - @PostConstruct 대신 애플리케이션 컨텍스트가 완전히 초기화된 후 실행되는 @EventListener(ApplicationReadyEvent.class) 사용

 - 메서드에 @Transactional을 부여하여 안정적인 데이터 저장 환경 확보