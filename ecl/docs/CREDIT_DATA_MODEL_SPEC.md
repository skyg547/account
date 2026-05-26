# 📂 신용리스크 데이터 모델 및 물리 컬럼 명세서

이 문서는 `credit-risk-service`에서 수행되는 8단계 신용리스크 산출 파이프라인(Process Flow)에 필요한 물리 테이블과 컬럼 명세를 정의합니다. 모든 테이블과 컬럼은 최신 바젤 III/IV 및 IFRS 9 기준을 준수합니다.

---

## 🏗️ 1. 엔티티 구조 및 관계 (ERD Concept)

- **CrCustomer (1) <---> (N) CrAccount**: 한 차주가 여러 계좌 보유
- **CrAccount (1) <---> (N) CrAccountCollateral**: 계좌-담보 간 배분 매핑
- **CrCollateral (1) <---> (N) CrAccountCollateral**: 담보의 여러 계좌 배분
- **CrAccount (1) <---> (N) CrRiskResult**: 계좌별 일자별 산출 결과

---

## 📋 2. 상세 물리 컬럼 명세

### 2.1 CrCustomer (차주 마스터)
| 물리 컬럼명 | 타입 (PG) | Null | PK | 설명 (비즈니스 의미) | 관련 Step |
|:---|:---|:---:|:---:|:---|:---:|
| id | BIGINT | N | Y | 내부 관리 ID (Auto Increment) | - |
| customer_code | VARCHAR(50) | N | - | 계정계 고객 번호 (Unique Key) | Step 1 |
| customer_name | VARCHAR(200)| N | - | 고객명/법인명 | Step 1 |
| customer_type | VARCHAR(30) | N | - | 고객 유형 (RETAIL, CORPORATE, SME) | Step 5 |
| internal_rating| VARCHAR(10) | Y | - | 내부 신용 등급 (예: AAA, 1등급 등) | Step 2, 5 |
| external_rating| VARCHAR(10) | Y | - | 외부 신용 등급 (S&P, Moody's 등) | Step 5 |
| industry_sector| VARCHAR(100)| Y | - | 산업 분류 (제조업, 서비스업 등) | Step 5 |
| country_code | VARCHAR(2)  | Y | - | 국가 코드 (ISO 3166-1) | Step 5 |
| is_sme | BOOLEAN | Y | - | 중소기업(SME) 여부 (RWA 산출 시 특례 적용) | Step 5 |

### 2.2 CrAccount (익스포저 원장)
| 물리 컬럼명 | 타입 (PG) | Null | PK | 설명 (비즈니스 의미) | 관련 Step |
|:---|:---|:---:|:---:|:---|:---:|
| id | BIGINT | N | Y | 내부 관리 ID (Auto Increment) | - |
| customer_id | BIGINT | N | - | CrCustomer 참조 FK | Step 1 |
| account_no | VARCHAR(50) | N | - | 계정계 계좌 번호 (Unique Key) | Step 1 |
| product_code | VARCHAR(20) | N | - | 상품 마스터 참조용 코드 | Step 1, 3 |
| outstanding_amt| NUMERIC(19,4)| N | - | 현 잔액 (On-Balance) | Step 3 |
| notional_amt | NUMERIC(19,4)| N | - | 총 약정액 (Limit) | Step 3 |
| currency | VARCHAR(3)  | N | - | 통화 코드 (KRW, USD 등) | Step 4 |
| open_date | DATE | N | - | 계좌 개설 일자 | Step 1 |
| maturity_date | DATE | Y | - | 만기 일자 (만기조정계수 b 산출 시 사용) | Step 5 |
| delinquent_days| INTEGER | Y | - | 현재 연체 일수 | Step 2 |
| internal_rating| VARCHAR(10) | Y | - | [고도화] 계좌별 개별 등급 (차주 등급보다 우선) | Step 5 |
| staging | VARCHAR(20) | Y | - | IFRS 9 스테이지 (STAGE1, 2, 3) | Step 2 |

### 2.3 CrCollateral (담보 마스터)
| 물리 컬럼명 | 타입 (PG) | Null | PK | 설명 (비즈니스 의미) | 관련 Step |
|:---|:---|:---:|:---:|:---|:---:|
| id | BIGINT | N | Y | 내부 관리 ID (Auto Increment) | - |
| collateral_code| VARCHAR(50) | N | - | 계정계 담보 번호 (Unique Key) | Step 1 |
| collateral_type| VARCHAR(30) | N | - | 담보 유형 (REAL_ESTATE, CASH, BOND 등) | Step 4 |
| appraisal_amt | NUMERIC(19,4)| N | - | 감정 평가 가액 (C) | Step 4 |
| base_haircut | NUMERIC(5,4) | N | - | 규제 기본 헤어컷 (Hc) | Step 4 |

### 2.4 CrRiskResult (산출 결과)
| 물리 컬럼명 | 타입 (PG) | Null | PK | 설명 (비즈니스 의미) | 관련 Step |
|:---|:---|:---:|:---:|:---|:---:|
| id | BIGINT | N | Y | 결과 레코드 ID | - |
| base_date | DATE | N | - | 리스크 기준일자 | Step 6 |
| account_id | BIGINT | N | - | CrAccount 참조 FK | Step 6 |
| staging | VARCHAR(20) | N | - | 산출 시점 스테이지 | Step 2 |
| ead | NUMERIC(19,4)| Y | - | 부도시 노출액 (EAD*) | Step 3 |
| pd | NUMERIC(10,8)| Y | - | 부도 확률 (PD) | Step 5 |
| lgd | NUMERIC(10,8)| Y | - | 부도시 손실률 (LGD) | Step 5 |
| expected_loss | NUMERIC(19,4)| Y | - | 기대 손실 (EL) | Step 6 |
| rwa_sa | NUMERIC(19,4)| Y | - | 위험가중자산 (표준방법) | Step 5 |
| rwa_irb | NUMERIC(19,4)| Y | - | 위험가중자산 (내부등급법) | Step 5 |
| status | VARCHAR(20) | N | - | 산출 상태 (COMPLETED, FAILED) | Step 6 |
| calculation_completed_at | TIMESTAMP | Y | - | 산출 완료 시시각 | Step 6 |

### 2.5 IRB/SA 규제 마스터 테이블 (Master Data)

규제 산식 고정 파라미터 및 맵핑 정보를 관리합니다.

#### 2.5.1 CrGradeMaster (IRB 부도율 마스터)
| 물리 컬럼명 | 설명 | 비즈니스 용도 |
|:---|:---|:---|
| rating_code | 등급 코드 (예: AAA, AA+) | 고객 등급별 PD 매핑 키 |
| pd_value | 부도 확률 (PD) | IRB 산식상의 핵심 변수 |
| notch_order | 등급 순서 (정수) | Staging 전이(Notch 하락) 판정 시 사용 |

#### 2.5.2 CrLgdSegmentMaster (IRB 손실률 마스터)
| 물리 컬럼명 | 설명 | 비즈니스 용도 |
|:---|:---|:---|
| customer_type | 차주 유형 | 세그먼트 분류 (LGD 차별화) |
| collateral_type | 담보 유형 | 담보 성격에 따른 손실 흡수율 차등 |
| lgd_value | 부도시 손실률 (LGD) | IRB 산식상의 핵심 변수 |

#### 2.5.3 CrSaRwMaster (SA 위험가중치 마스터)
| 물리 컬럼명 | 설명 | 비즈니스 용도 |
|:---|:---|:---|
| customer_type | 차주 유형 | 표준방법 위험가중치 결정 키 |
| rating_code | 등급 코드 | 기업/은행 등급별 차등 RW |
| risk_weight | 위험가중치 (RW) | 표준방법 RWA 산출 배수 |

#### 2.5.4 CrRegulatoryParameter (규제 파라미터)
| 물리 컬럼명 | 설명 | 비즈니스 용도 |
|:---|:---|:---|
| param_key | 규제 상수 키 | PD Floor, Correlation Base 등 |
| param_value | 상수값 | 하드코딩 대체용 파라미터 |

---
*Senior Fintech Engineer Agent: Antigravity*
