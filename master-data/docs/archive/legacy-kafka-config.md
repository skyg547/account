# Legacy Kafka configuration note

과거 `config-repo/master-data.yml`에는 `management.kafka` 아래 Kafka producer/consumer 설정이 있었습니다.

이 설정은 Spring Boot가 인식하는 `spring.kafka` 경로가 아니었고, `master-data`에도 이벤트 producer/consumer 또는 outbox 포트 구현이 없어 실제 업무 흐름과 연결되지 않았습니다. 설정만 존재해 Kafka 연동이 동작하는 것처럼 보이는 스켈레톤을 피하려고 활성 설정에서 제거했습니다.

기존 값의 의미는 다음과 같습니다.

- broker: 로컬 `localhost:9092`
- consumer group: `master-data-group`
- key/value serializer: Spring Kafka String/JSON 조합
- offset reset: `earliest`

실제 이벤트 연동을 추가할 때는 먼저 core 출력 포트와 outbox/inbox 멱등 정책, 이벤트 스키마와 재처리 기준을 구현한 뒤 adapter 설정을 `spring.kafka` 아래에 복원해야 합니다. 단순 설정 복원만으로는 업무 이벤트가 발행되지 않습니다.