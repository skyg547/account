# closing docs

`closing` 모듈은 결산 캘린더, 태스크, 게이트, 기간 잠금, 재오픈 승인, 평가/충당 배치, 결산 조정을 관리한다.

ECL 충당 배치는 외부 ECL 산출 결과 포트(`EclAllowanceResultPort`)로 목표 충당금을 조회하고, 기존 GL 대손충당금 잔액과의 차이만 보충/환입 전표로 처리한다. Stage/PD/LGD/EAD 계산은 `closing`에서 수행하지 않는다.

읽기 순서:

1. [beginner-guide.md](/C:/Users/skyg547/IdeaProjects/account/closing/docs/beginner-guide.md)
2. [process-flow.md](/C:/Users/skyg547/IdeaProjects/account/closing/docs/process-flow.md)
3. [schema.md](/C:/Users/skyg547/IdeaProjects/account/closing/docs/schema.md)
