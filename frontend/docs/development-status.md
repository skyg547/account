# 📊 프로젝트 개발 현황 (Development Status)

본 문서는 시스템의 모듈별 화면 개발 및 API 연동 상태를 실시간으로 관리합니다.

- **완성 기준:** UI 레이아웃 구현 + 백엔드 API 연동 + 데이터 바인딩 완료.
- **상태 표기:** 
    - ✅ **완성 (Full Stack):** UI 및 API 연동 완료
    - 🎨 **진행 중 (UI Only):** UI 스켈레톤 구현 완료 (Mock Data)
    - ⏳ **대기 (Pending):** 개발 예정

---

## 1. 대시보드 (DASHBOARD)
| 화면명 | 경로 | 상태 | API 연동 | 비고 |
| :--- | :--- | :---: | :---: | :--- |
| 통합 대시보드 | `/` | 🎨 | [ ] | 주요 KPI 위젯 구현 완료 |

## 2. 재무 업무 (ACCOUNTING)
| 화면명 | 경로 | 상태 | API 연동 | 비고 |
| :--- | :--- | :---: | :---: | :--- |
| 전표 조회 | `/journal/list` | ✅ | [X] | 백엔드 API `/api/journals` GET 연동 완료 |
| 전표 입력 | `/journal/entry` | ✅ | [X] | 백엔드 API `/api/journals` POST 및 대차 검증 연동 완료 |
| 총계정원장 | `/journal/ledger/gl` | 🎨 | [ ] | 계정별 합계 그리드 완료 |
| 보조원장 | `/journal/ledger/sl` | 🎨 | [ ] | 거래처별 잔액 그리드 완료 |
| 자동 분개 설정 | `/journal/rules` | 🎨 | [ ] | 룰 설정 UI 스켈레톤 |
| 결산 관리 | `/closing` | 🎨 | [ ] | 타임라인식 마감 프로세스 UI |

## 3. 재무 운영 (OPERATIONS)
| 화면명 | 경로 | 상태 | API 연동 | 비고 |
| :--- | :--- | :---: | :---: | :--- |
| 매출채권(AR) 관리 | `/finance/receivable` | ✅ | [X] | 백엔드 API `/api/receivable/invoices` 연동 완료 |
| 매입채무(AP) 관리 | `/finance/payable` | ✅ | [X] | 백엔드 API `/api/payable/invoices` 연동 완료 |
| 세무/부가세 | `/finance/tax` | 🎨 | [ ] | 부가세 신고 기초 데이터 |
| 고정자산 관리 | `/finance/assets` | ✅ | [X] | 자산 대장, 등록 및 처분 기능 |
| 지출결의(경비) 포털 | `/finance/expense` | 🎨 | [ ] | 개인별 지출 현황 및 신청 |
| 예산 관리 | `/finance/budget` | 🎨 | [ ] | **[NEW]** 부서별 예산 편성 |
| 자금 수지 계획 | `/finance/cashflow` | 🎨 | [ ] | **[NEW]** 입출금 예정 스케줄 |
| 리스 회계 | `/finance/lease` | ✅ | [X] | **[NEW]** IFRS 16 리스 등록 및 재측정 |
| 자션/리스 결산 | `/finance/assets/closing` | ✅ | [X] | **[NEW]** 월말 상각 및 결산 실행 |
| 연결 회계 기초 | `/finance/consolidation` | 🎨 | [ ] | **[NEW]** 연결 정산표 기초 |
| 결산 관리 | `/closing` | ✅ | [X] | **[NEW]** 외화 평가 및 대손충당금(ECL) 배치 실행 API 연동 완료 |

## 4. 은행 특화 업무 (BANKING)
| 화면명 | 경로 | 상태 | API 연동 | 비고 |
| :--- | :--- | :---: | :---: | :--- |
| 지점간 자금 정산 | `/finance/banking/inter-branch` | ✅ | [X] | 백엔드 정산 대시보드 API 연동 완료 |
| 외환(FX) 포지션 | `/fx/position` | ✅ | [X] | FX 대시보드 API 연동 완료 |
| 내부 금리(FTP) | `/finance/banking/ftp` | 🎨 | [ ] | 부서 성과 기여도 분석 |
| 은행 일계표 | `/finance/banking/daily-summary` | 🎨 | [ ] | 일일 시산표 레이아웃 |
| 감사 모니터링 | `/finance/banking/audit` | 🎨 | [ ] | 실시간 위험 거래 알림 |
| Basel III RWA 현황 | `/risk/basel-iii/rwa` | ✅ | [ ] | **[NEW]** RWA 산출 및 모니터링 |
| IFRS 9 ECL 시뮬레이션 | `/risk/ifrs-9/ecl` | ✅ | [ ] | **[NEW]** 기대신용손실 분석 |
| ALM / 금리 리스크 | `/risk/alm/interest` | ✅ | [ ] | **[NEW]** 금리 갭 및 NII 시뮬레이션 |
| 유동성 리스크(LCR) | `/risk/liquidity` | ✅ | [ ] | **[NEW]** LCR/NSFR 비율 관리 |

## 5. 기준 정보 (MASTER)
| 화면명 | 경로 | 상태 | API 연동 | 비고 |
| :--- | :--- | :---: | :---: | :--- |
| 계정 과목 관리 | `/master/account` | ✅ | [X] | 백엔드 API `/api/basic/account-subjects` 연동 완료 |
| 거래처 관리 | `/master/partner` | ✅ | [X] | 백엔드 API `/api/basic/business-partners` 연동 완료 |
| 기준 정보 승인 | `/master/approval` | 🎨 | [ ] | 승인 대기함 및 Diff 뷰어 |

## 6. 시스템 관리 (ADMIN)
| 화면명 | 경로 | 상태 | API 연동 | 비고 |
| :--- | :--- | :---: | :---: | :--- |
| 재무제표 보고서 | `/reports/statements` | ✅ | [X] | 백엔드 API `/api/v1/reporting/generate` 연동 및 Mock 제거 완료 |
| 사용자 및 권한 | `/admin/users` | 🎨 | [ ] | 사용자 목록 및 롤 매핑 |
| 귀속 부서 관리 | `/admin/dept` | 🎨 | [ ] | 조직도 및 귀속 정보 |
| 시스템 로그 조회 | `/admin/logs` | 🎨 | [ ] | 통합 이벤트 로그 조회 |

## 7. 인프라 및 가이드 (INFRA)
| 구분 | 문서/링크 | 상태 | 비고 |
| :--- | :--- | :---: | :--- |
| 프론트 가이드 | `docs/frontend-engineering-guide.md` | ✅ | **[NEW]** Tailwind 표준 정의 |
| 스타일 표준 | `Tailwind CSS 3.4` | ✅ | **[NEW]** CSS 모듈에서 전환 중 |

---

**업데이트 일자:** 2026-04-23 16:15 (Tailwind CSS 표준 도입 완료)
