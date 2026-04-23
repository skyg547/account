# 🛠️ 프론트엔드 엔지니어링 가이드 (v1.0)

본 문서는 **'Account.AI'** 프로젝트의 프론트엔드 개발 표준 및 품질 가이드를 정의합니다.

## 1. 기술 스택 (Tech Stack)
- **Framework:** Next.js 15 (App Router)
- **Styling:** Tailwind CSS (Utility-first)
- **State Management:** React Context API (NavContext 등)
- **Icons:** Lucide React
- **Charts:** Recharts

## 2. 디자인 시스템 및 테마
### 🎨 컬러 팔레트 (Tailwind Config 연동)
- **Primary:** `#3b82f6` (Blue-500) - 메인 액션 및 브랜드 컬러
- **Background:** `var(--background)` - 다크 모드 연동 기본 배경
- **Surface:** `glass-card` (커스텀 클래스) - 글래스모피즘 카드 UI

### ✨ 주요 UI 유틸리티
- **Glassmorphism:** `bg-white/5 backdrop-blur-md border border-white/10`
- **Modern Shadow:** `shadow-xl shadow-black/20`
- **Hover Effects:** `hover:scale-[1.02] transition-all duration-300`

## 3. 컴포넌트 개발 규칙
- **Functional Components:** 모든 컴포넌트는 Functional 레시피로 작성하며, `use client` 지시어를 적절히 사용합니다.
- **Tailwind-first:** 인라인 클래스 사용을 원칙으로 하며, 복잡한 조합은 `@apply` 보단 컴포넌트 분리를 지향합니다.
- **Accessibility:** 모든 버튼과 인터랙션 요소에는 고유한 `id`와 `aria-label`을 부여합니다.

## 4. API 연동 가이드 (Integration)
- 모든 API 요청은 `src/lib/api.ts`에 정의된 중앙 클라이언트를 사용합니다.
- 데이터 로딩 시 `Skeleton Screen` 또는 `Loading Spinner`를 반드시 표시합니다.
- 에러 발생 시 `Toast` 또는 `AlertBox`를 통해 사용자에게 친절한 메시지를 제공합니다.

## 5. 작업 프로세스
1. **Layout 정의**: `app/` 하위에 라우트 생성
2. **UI 스켈레톤**: Tailwind로 레이아웃 및 Mock 데이터 배치
3. **API 바인딩**: 실데이터 연동 및 상태 관리
4. **검증**: 다크모드 및 반응형 레이아웃 확인

---
**Last Updated:** 2026-04-23 by [프론트]
