# Internal Audit Module (내부감사 모듈)

## 📌 개요
내부회계관리제도(ICFR/K-SOX) 컴플라이언스 및 재무/권한 변경 이력 감사 로그를 담당하는 모듈입니다.

## 🏗 아키텍처 (헥사고날 구조)
- internal-audit:core: 감사 도메인 엔티티, 유즈케이스, 영속성 어댑터
- internal-audit:api: 감사 로그 조회 REST Controller 및 API DTO
- internal-audit:batch: 배치 감사 점검 및 집계 오케스트레이터

## ⚙️ 주요 기능
1. **감사 트레일 (Audit Trail)**: 데이터 CUD 발생 시 변경 전/후 스냅샷 기록
2. **SoD (Separation of Duties) 검증**: 직무 분리 위반 탐지
