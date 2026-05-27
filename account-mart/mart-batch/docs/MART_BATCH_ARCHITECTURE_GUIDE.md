# Risk Data Mart Batch Architecture Guide

본 문서는 리스크 데이터 마트(`mart-batch`)의 ETL 공정 및 트리거 구조를 설명합니다.

## 1. 개요 (Overview)
`mart-batch`는 원천 시스템(ODS)의 파편화된 데이터를 대손충당금(IFRS9) 산출 엔진이 즉시 사용할 수 있는 표준 구조(CDM: Common Data Model)로 정규화하여 적재하는 역할을 수행합니다.

## 2. 배치 트리거 및 실행 순서 (Pipeline Flow)
`integratedPositionEtlJob`은 총 7단계의 공정으로 구성됩니다.

| 순서 | Step 명칭 | 비즈니스 의미 | 주요 수행 내용 |
|:---:|:---:|:---|:---|
| 1 | `preProcessStep` | **멱등성 보장** | 기준일자의 기존 마트 데이터를 삭제하여 중복 적재 방지 |
| 2 | `regulatorySyncStep` | **마스터 동기화** | 규제 파라미터 및 환율, 상품/고객 마스터 정보 최신화 |
| 3 | `ledgerDataQualityStep` | **데이터 품질 검증** | 원장 데이터의 필드 누락, 형식 오류 등을 전수 검사 |
| 4 | `collateralDataQualityStep` | **담보 품질 검증** | 담보 데이터의 유효성 및 평가액 적정성 검증 |
| 5 | `odsReconcileStep` | **회계 대사** | 원천 원장(SL)과 총계정원장(GL)의 합계 일치 여부 확인 |
| 6 | `cdmLoadStep` | **CDM 적재** | `mart-core`의 Processor를 사용하여 통합 리스크 포지션 생성 |
| 7 | `martReportingStep` | **리포팅 집계** | 산출된 데이터를 바탕으로 경영진 보고용 지표 사전 집계 |

## 3. 핵심 기술 요소
- **Parallel Processing**: `mart.batch.cdm-load.parallel-enabled=true` 설정을 통해 멀티스레드 기반의 초고속 적재가 가능합니다.
- **Fail-Tolerant**: 데이터 변환 중 일부 오류가 발생해도 `skipLimit` 설정을 통해 전체 배치가 중단되지 않도록 설계되었습니다.
- **Zero-Config Seed**: `mart.batch.demo-seed.enabled=true` 시 테스트용 ODS 데이터를 자동으로 생성하여 즉시 검증이 가능합니다.

---
💡 **[초보자 가이드]**
ETL이란 Extract(추출), Transform(변환), Load(적재)의 약자입니다. 재무 결산 시스템에서는 원천 데이터가 복잡하기 때문에, '마트'라는 중간 창고에 예쁘게 정리해두어야 금리/신용 결산 대손 엔진이 빠르게 계산을 수행할 수 있습니다.
