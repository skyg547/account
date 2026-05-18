# Reconciliation Beginner Guide

## 1. 대사가 무엇인가요 (초보자를 위한 개념 설명)

대사(Reconciliation)는 쉽게 말해 "두 장부가 서로 맞는지 짝을 맞추어 확인하는 일"입니다.
마치 우리가 식당에서 밥을 먹고 영수증을 받았을 때, 내 신용카드 결제 앱에 찍힌 금액과 영수증 금액이 똑같은지 비교해보는 것과 같습니다.

예시:
- **은행 입출금 내역**과 우리 회사의 **회계장부**가 맞는지 확인
- **보조원장(세부 내역)**과 **총계정원장(전체 요약)**이 맞는지 확인
- **원천 시스템(예: 대출 시스템) 금액**과 **회계 전표 금액**이 맞는지 확인

이 모듈은 이런 비교 작업을 사람이 일일이 하지 않도록 자동화하고, 만약 차이(금액이 다르거나 누락된 경우)가 발생하면 그 원인을 추적하고 수정할 수 있도록 돕는 역할을 합니다.

## 2. 초보자가 먼저 알아야 할 용어

- **대사 단위 (Reconciliation Unit):** 무엇과 무엇을 비교할지 묶어놓은 그룹입니다. (예: "A은행 입출금 대사")
- **대사 규칙 (Reconciliation Rule):** 어떤 조건일 때 '같다'고 인정할지 정한 기준입니다. (예: "금액이 10원 이하로 차이나면 같은 것으로 무시하자")
- **대사 실행 (Reconciliation Run):** 실제로 규칙을 적용해서 비교를 돌린 1회의 작업 결과입니다.
- **차이 (Difference):** 비교해봤더니 서로 맞지 않아서 튕겨져 나온 항목입니다.
- **사유코드 (Reason Code):** 왜 차이가 났는지 이유를 분류해놓은 코드입니다. (예: "수수료 누락", "날짜 차이")
- **조정분개 (Adjustment Journal Entry):** 차이가 나는 금액을 회계 장부에 맞춰주기 위해 시스템이 자동으로 끊어주는 추가 전표입니다.
- **SLA (Service Level Agreement):** 이 차이를 며칠 안에 해결해야 하는지 정해놓은 기한입니다.

## 3. 실무 흐름을 쉬운 말로 보면

1. "어떤 비교를 할지" 대사 단위를 만듭니다.
2. "어떻게 같다고 볼지" 규칙을 만듭니다.
3. 대사를 실행합니다.
4. 맞지 않으면 차이를 생성합니다.
5. 담당자를 배정합니다.
6. 사유를 정하고 필요하면 조정분개를 연결합니다.
7. 해결되면 `RESOLVED`, 의미 없는 차이면 `IGNORED`로 마감합니다.

## 4. 현재 헥사고날 아키텍처와 시스템 환경

현재 시스템은 **헥사고날 아키텍처(Hexagonal Architecture, Ports and Adapters)**를 따르고 있습니다.
- 핵심 비즈니스 로직(대사 엔진)은 내부에 두고, 외부 시스템(전표, 원장 등)과의 통신은 모두 **Port(인터페이스)**와 **Adapter(구현체)**를 통해 이루어집니다.
- **ID 기반 참조 (ID-based references):** 다른 모듈의 데이터를 가져올 때 객체 전체를 가져오지 않고, ID 값(예: `journalEntryId`)만 저장하여 모듈 간 결합도를 낮췄습니다.
- **다단계 도커 환경 (Multi-stage Docker):** 애플리케이션 빌드와 실행 환경을 분리하여 컨테이너를 가볍고 안전하게 유지합니다.
- 데이터 변경 이력은 **SCD2 (Slowly Changing Dimensions)** 방식을 적용하여, 과거의 상태와 현재의 상태를 모두 추적할 수 있도록 설계 방향을 잡고 있습니다.

## 5. 현재 코드에서 꼭 알아야 할 현실

- 메인 대사 실행은 더미 금액 대신 `criteriaJson`의 원천 집계값과 전표 조회 포트(`JournalQueryPort`)의 대상 집계값을 비교합니다.
- 자동 조정분개가 필요한 경우 대사 서비스가 직접 DB에 저장하지 않고 전표 생성 포트(`JournalPostingPort`)로 위임하여 ID만 받아옵니다.
- 심화 대사 흐름은 `matchingRulesJson`의 SOURCE/INTERFACE 집계값과 전표/원장 조회 포트를 함께 사용합니다.

## 6. 자동 매칭은 지금 어떻게 동작하나요

현재 `AutomatedMatchingEngine`는 기본적으로 금액과 일자를 중심으로 봅니다.
- 기본 호출에서는 날짜가 하루만 달라도 실패합니다.
- 옵션 호출에서는 예를 들어 "1일 이내, 1.00원 이내" 같은 허용오차 기준을 줄 수 있습니다.
- 복합 옵션을 켜면 은행 적요에 포함된 전표번호, 전표 적요 문구, 계좌번호까지 함께 볼 수 있습니다.

조정분개를 자동으로 만들 때는 차변/대변 계정코드를 반드시 명시해야 합니다. 계정코드가 없으면 시스템이 임의 계정으로 맞추지 않고 실패시켜 잘못된 전기를 막습니다.

## 7. 처음 호출해보기 좋은 API 순서

1. `POST /api/reconciliation/units`
2. `POST /api/reconciliation/rules`
3. `POST /api/reconciliation/reason-codes`
4. `POST /api/reconciliation/run`
5. `GET /api/reconciliation/runs/{runId}/differences`
6. `POST /api/reconciliation/differences/assign`
7. `POST /api/reconciliation/differences/resolve`

## 8. 문서 추천 순서

1. [README.md](./README.md)
2. [process-flow.md](./process-flow.md)
3. [schema.md](./schema.md)

이 순서로 보면 대사의 의미, 실행 흐름, 저장 구조를 한 번에 잡기 좋습니다.
