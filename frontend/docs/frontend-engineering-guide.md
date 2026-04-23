# 🛠️ 프론트엔드 엔지니어링 가이드 (Grid CRUD & API 연동)

본 가이드는 우리 프로젝트의 개발 효율성을 높이고, 모든 개발자가 일관된 코딩 스타일을 유지하기 위한 기술 표준입니다.

---

## 1. API 호출 표준 (Axios & TanStack Query)

우리는 서버와의 통신을 위해 `Axios`를 기반으로 한 공통 인스턴스를 사용하며, 데이터 상태 관리를 위해 `TanStack Query (v5)`를 도입했습니다.

### ① API 호출 유틸리티 (`/src/lib/api.ts`)
모든 요청은 공통 Axios 인스턴스를 거쳐야 하며, 인터셉터를 통해 JWT 토큰 주입 및 에러 처리를 자동화합니다.

```typescript
// 예시: 전표 데이터 가져오기
export const fetchJournals = async () => {
  const { data } = await api.get('/api/journal/list');
  return data;
};
```

### ② TanStack Query 사용 패턴
컴포넌트 내에서는 직접 `useEffect`를 쓰지 말고, 전용 Hook을 사용하세요.

- **조회 (Read):** `useQuery`를 사용하세요. 가시적인 로딩 상태(`isLoading`)를 반드시 처리해야 합니다.
- **생성/수정/삭제 (CUD):** `useMutation`을 사용하세요. 성공 시 `queryClient.invalidateQueries`를 호출해 데이터를 최신화해야 합니다.

---

## 2. Grid CRUD 구현 가이드 (표준 패턴)

회계 시스템의 90%는 '그리드(표)'로 이루어집니다. 엑셀처럼 편리한 CRUD 경험을 제공하기 위한 표준 구현 방식입니다.

### [그리드 CRUD 프로세스]
1.  **Read:** `useQuery`로 목록 데이터를 가져와 `state`에 담습니다.
2.  **Create:** '행 추가' 버튼 클릭 시 `state` 배열에 빈 객체를 새롭게 추가(`push`)합니다.
3.  **Update:** 각 셀(Cell)의 `input`에 `onChange` 이벤트를 걸어 `state`를 실시간 업데이트합니다.
4.  **Delete:** '행 삭제' 버튼 클릭 시 필터링을 통해 `state`에서 해당 항목을 제거합니다.
5.  **Save:** `onBlur` 또는 상단 '저장' 버튼 클릭 시 `useMutation`을 호출해 서버에 반영합니다.

```tsx
// 그리드 행 추가 예시
const addRow = () => {
  const newRow = { id: Date.now(), accountCode: '', debit: 0, credit: 0 };
  setData([...data, newRow]);
};
```

---

## 3. 이벤트 핸들링 및 상태 관리

- **대차 차액 검증 (Banking Integrity):** 차변과 대변의 합계가 맞지 않으면 실시간으로 경고 메시지를 보여주고 '저장' 버튼을 비활성화하세요.
- **입력 제한:** 금액 입력 칸에는 숫자만 들어가야 하며, 천 단위 콤마(`,`) 처리를 위한 필터 유틸리티를 사용하세요.
- **모달/오버레이:** 복잡한 검색(예: 수만 개의 계정 과목 중 하나 찾기)은 필터가 포함된 모달 오버레이를 사용합니다.

---

## 💡 초보자를 위한 개념 설명: "그리드 CRUD"
> **그리드(Grid)란?** 엑셀 시트처럼 칸이 나뉘어 있는 표를 말합니다.
> **CRUD란?** 생성을 뜻하는 **C**(reate), 조회를 뜻하는 **R**(ead), 수정을 뜻하는 **U**(pdate), 삭제를 뜻하는 **D**(elete)의 앞 글자를 딴 용어로, 데이터 관리의 기본이 되는 4가지 기능을 의미합니다.
> 
> 우리가 만드는 시스템에서는 은행원들이 엑셀을 쓰듯이 웹 브라우저에서도 데이터를 넣고(C), 보고(R), 고치고(U), 지울(D) 수 있게 만드는 것이 핵심입니다!

---

**작성자: [프론트]**
*최종 수정일: 2026-04-22*
