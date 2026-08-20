# 🛠️ 프론트엔드 실전 개발 가이드 (Next.js 15 & Tailwind v4)

이 문서는 새로운 금융 도메인 화면을 추가하거나 기존 화면을 수정할 때 사용하는 실전 개발 매뉴얼입니다.

---

## 1. 🚀 새로운 화면 추가하기 (Step-by-Step)

예시: 자금운영 카테고리에 **외화 송금 신청(`/expenditure/fx-transfer`)** 화면 만들기

### Step 1. 라우트 폴더 및 `page.tsx` 생성
`src/app/expenditure/fx-transfer/page.tsx` 파일을 생성합니다.

```tsx
'use client';

import React, { useState } from 'react';
import { Send, Building2, DollarSign } from 'lucide-react';
import PageHeader from '@/components/ui/PageHeader';
import AmountDisplay from '@/components/ui/AmountDisplay';
import StatusBadge from '@/components/ui/StatusBadge';

export default function FxTransferPage() {
  const [amount, setAmount] = useState(50000);

  return (
    <div className="space-y-8 pb-16">
      {/* 1. 표준 페이지 헤더 */}
      <PageHeader
        title="외화 송금 신청"
        description="해외 거래처 결제를 위한 외화 송금 신청 및 실시간 환율을 조회합니다."
        breadcrumbs={[
          { label: '자금운영' },
          { label: '외화 송금' }
        ]}
        icon={Send}
      />

      {/* 2. KBank 스타일 화이트/다크 카드 */}
      <div className="rounded-2xl bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800 p-7 shadow-xs space-y-6">
        <h3 className="text-base font-bold text-[#17191e] dark:text-slate-100">
          송금 기본 정보
        </h3>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-[#17191e] dark:text-slate-200">송금액 (USD)</label>
            <input
              type="number"
              value={amount}
              onChange={(e) => setAmount(Number(e.target.value))}
              className="w-full bg-[#f7f8fb] dark:bg-slate-900 border border-[#eaedf4] dark:border-slate-700 rounded-xl px-4 py-2.5 text-xs text-[#17191e] dark:text-slate-100 font-mono font-bold"
            />
          </div>
        </div>

        <div className="flex justify-end">
          <button className="px-6 py-3 bg-[#4262ff] hover:bg-[#3452e6] rounded-xl text-white text-xs font-bold transition-all shadow-md shadow-blue-600/20">
            송금 신청 확정
          </button>
        </div>
      </div>
    </div>
  );
}
```

### Step 2. 사이드바 메뉴 등록
[`src/components/layout/menus.ts`](file:///c:/dev/account/frontend/src/components/layout/menus.ts)의 해당 카테고리(`OPERATIONS`)에 메뉴 항목을 등록합니다.

```typescript
{
  category: 'OPERATIONS',
  group: '자금 집행 & 외환',
  module: 'expenditure',
  items: [
    // ...
    { label: '외화 송금', href: '/expenditure/fx-transfer', icon: 'Send' },
  ]
}
```

---

## 2. 🧩 주요 공통 UI 컴포넌트 사용법

| 컴포넌트 | 용도 | 사용 예시 |
| :--- | :--- | :--- |
| **`PageHeader`** | 화면 제목, 설명, 빵부스러기(Breadcrumbs) | `<PageHeader title="전표 목록" description="..." breadcrumbs={[{ label: '재무회계' }, { label: '전표' }]} icon={FileText} />` |
| **`AmountDisplay`** | 통화 포맷팅 및 정밀 금액 표시 | `<AmountDisplay amount={12450000} />` |
| **`StatusBadge`** | 승인 상태, 결재 단계 뱃지 | `<StatusBadge status="승인완료" variant="success" />` |
| **`Tabs`** | 세그먼트 컨트롤 탭 | `<Tabs tabs={items} activeTab={active} onChange={setActive} />` |
| **`EmptyState`** | 데이터 없음 안내 카드 | `<EmptyState icon={Inbox} title="내역이 없습니다" description="새로운 전표를 등록해보세요." />` |
| **`LoadingSkeleton`**| 데이터 로딩 플레이스홀더 | `<LoadingSkeleton rows={4} height="h-14" />` |

---

## 3. 🌙 다크 모드 작성 규칙

* 모든 카드 및 컨테이너에는 기본 화이트 배경과 다크 모드 배경을 함께 부여합니다.
  ```tsx
  className="bg-white dark:bg-[#131b2e] border border-[#eaedf4] dark:border-slate-800"
  ```
* 텍스트 컬러:
  ```tsx
  className="text-[#17191e] dark:text-slate-100" // 제목/핵심 텍스트
  className="text-[#545b69] dark:text-slate-400" // 서브텍스트
  ```
* 인풋/셀렉트:
  ```tsx
  className="bg-[#f7f8fb] dark:bg-slate-900 border border-[#eaedf4] dark:border-slate-700 text-[#17191e] dark:text-slate-100"
  ```
