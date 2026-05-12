# 💸 Expenditure Resolution Service (지출 결의)

`expenditure-resolution` 모듈은 회사에서 돈을 쓰기 위해 기안을 올리고, 승인받아 최종적으로 돈을 지급하기까지의 프로세스(지출 결의 및 AP)를 관리합니다.

---

## 1. 🐣 초보자를 위한 개념 설명 (Beginner Guide)

회사에서 비품을 사거나 회식비를 쓸 때 마음대로 돈을 꺼내 쓸 수 없습니다.
1. **지출 결의서 작성:** "개발팀 회식비로 30만 원 쓰겠습니다" 하고 문서를 올립니다.
2. **예산 통제 (Budget Control):** 개발팀에 배정된 회식비 예산이 30만 원 이상 남아있는지 확인합니다.
3. **세금계산서 매핑:** 돈을 썼으면 적격 증빙(세금계산서 등)을 붙여야 합니다. `tax` 모듈을 참조합니다.
4. **승인 및 전표 생성:** 팀장이 승인하면, `journal-ledger`에 "회식비 30 / 미지급금 30" 전표가 꽂힙니다.
5. **지급 (AP Payment):** 실제 통장에서 돈이 나가면 미지급금을 지우는 지급 처리를 합니다.

---

## 2. 🔄 최근 고도화 내용 및 아키텍처

- **마스터 참조 완결성:** 예전에는 부서나 계정코드가 잘못 입력되어도 몰래 넘어가는(`orElse(null)`) 문제가 있었지만, 지금은 `MasterDataQueryPort`를 통해 엄격하게 검증(`orElseThrow()`)하여 잘못된 전표가 생성되지 않습니다.
- **DTO 이름 자동 바인딩:** API 응답 시 부서명, 계정명이 항상 `null`로 나가던 문제를 해결하여, Assembler 단계에서 `master-data`를 조회해 이름을 채워 넣습니다.
- **리스 지급 계정 분리 (IFRS16):** `asset-lease` 모듈의 요청을 받아 리스료를 지급할 때, 원금과 이자를 하나의 계정에 뭉뚱그리지 않고 분리해서 차변에 반영하는 로직이 적용되었습니다.

---

## 3. 🐳 실행 방법 (Docker & Local)

**최신 엔터프라이즈 Docker 환경 (권장):**
```bash
docker-compose up -d expenditure-resolution
```

**로컬 개발 환경 (전통적 방식):**
```bash
./gradlew :expenditure-resolution:api:bootRun
```
