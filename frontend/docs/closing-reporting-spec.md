# 결산(Closing) & 보고서(Reporting) 도메인 설계서

> **Issue**: #10
> **브랜치**: `feature/frontend-ui-update`

## 1. 개요
결산(Closing) 프로세스 관리 화면 7개와 보고서(Reporting) 조회 및 내보내기 화면 4개를 정의하는 설계서입니다.

## 2. 화면 목록

### 2.1. 결산관리 (CLOSING)
| 경로 | 화면 구성 및 주요 기능 |
|---|---|
| `/closing/calendar` | 월별/연도별 결산 캘린더, 마일스톤 타임라인 뷰, 결산 진척도 프로그레스 |
| `/closing/tasks` | 결산 태스크 체크리스트, 담당자 배정, 태스크별 상태(대기/진행/완료/지연) 뱃지 |
| `/closing/gates` | 결산 게이트(선행 조건) 점검 매트릭스, 시스템 자동 체크 상태 |
| `/closing/period-lock` | 회계 기간(Period) 잠금/재개 요청 워크플로우 폼 및 이력 테이블 |
| `/closing/valuation` | 외화 평가 배치 관제(실행, 상태 모니터링, 결과 요약 뷰) |
| `/closing/adjustment` | 결산 조정 전표 전용 등록/조회 UI (기본 전표와 유사하나 결산 속성 포함) |
| `/closing/annual` | 연차 결산(이익잉여금 대체 등) 실행 패널 및 손익 대체 결과 요약 |

### 2.2. 보고서관리 (REPORTING)
| 경로 | 화면 구성 및 주요 기능 |
|---|---|
| `/reports/statements` | 재무상태표(BS), 손괄손익계산서(IS) 동적 트리 그리드 뷰, 연도/월별 비교 |
| `/reports/export` | 다중 보고서 선택, 포맷(PDF/Excel/CSV) 선택 및 비동기 다운로드 내역 테이블 |
| `/reports/disclosure-notes` | 주석(Disclosure) 정보 마트, 공시용 매트릭스 및 전표 드릴다운 |
| `/reports/regulatory` | 금감원(FSS)/한국은행 등 규제 보고용 서식 생성 및 제출 이력 |

## 3. Mock 데이터 명세 (`src/mocks/closing.ts`, `src/mocks/reporting.ts`)

### 3.1. 결산 (Closing)
- `ClosingTaskDto`: { id, name, assignee, status, dueDate, progress }
- `PeriodDto`: { periodId, name, status: 'OPEN' | 'CLOSED' | 'LOCKED', lockedAt }
- 결산 태스크 목록, 기간 잠금 이력 데이터.

### 3.2. 보고서 (Reporting)
- `FinancialStatementDto`: { accountCode, accountName, amountCurrent, amountPrevious, variance }
- BS/IS 트리 구조 데이터 및 다운로드 히스토리 데이터.

## 4. UI 컴포넌트 활용 계획
- **StatusBadge**: 태스크 상태, 결산 기간 상태, 평가 배치 상태
- **Tabs**: 재무제표 탭(BS/IS/CF), 결산 태스크 탭(진행중/완료)
- **LoadingSkeleton**: 재무제표 렌더링 지연 시스켈레톤
