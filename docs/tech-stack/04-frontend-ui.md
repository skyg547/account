# 🔄 [Tech 07] TanStack Query (React Query)

### 👶 초보자를 위한 개념 설명
> **TanStack Query란 무엇인가요?**
> 마치 **"똑똑한 배달 앱 관리자"**와 같습니다. 음식을 주문(서버 데이터 요청)하면 배달 현황을 알려주고, 금방 시킨 음식은 기억해 뒀다가 다시 물어보면 주방(서버)에 다시 안 물어보고 바로 꺼내줍니다.

### 💡 주요 특징 및 예시
1. **Caching**: 한 번 가져온 데이터는 메모리에 보관해서 화면을 바꿀 때 번쩍이는 현상 없이 바로 보여줍니다.
2. **Synchronization**: 서버 데이터가 바뀌면 자동으로 내 화면도 최신 버전으로 업데이트해줍니다.
3. **Loading Status**: 데이터가 오는 동안 빙글빙글 도는 로딩 표시를 아주 쉽게 구현할 수 있습니다.

### 🛠️ 우리 프로젝트에서의 활용
- **Data Hook**: `frontend/docs/frontend-engineering-guide.md`에 정의된 대로 모든 서버 API 호출은 이 라이브러리를 통해 관리하여 프론트엔드 성능을 높였습니다.

### 🚀 실전 사용 가이드 (Step-by-Step)
**[서버에서 데이터 가져오기]**
1. **Query Hook 생성**: `const { data } = useQuery({ queryKey: ['users'], queryFn: fetchUsers })`를 호출합니다.
2. **로딩 처리**: `isLoading` 상태를 사용하여 "불러오는 중..." 메시지를 띄웁니다.
3. **데이터 출력**: 가져온 `data`를 그리드나 리스트에 뿌려줍니다.

---

# 🎨 [Tech 08] Lucide Icons & Vanilla CSS Modules

### 👶 초보자를 위한 개념 설명
> **이것들은 무엇인가요?**
> - **Lucide**: 웹사이트의 이정표 역할을 하는 **"세련된 아이콘 세트"**입니다.
> - **CSS Modules**: 우리 집(컴포넌트) 안에서만 쓰는 **"전용 옷장"**입니다. 다른 집 옷하고 섞이지 않게 이름표를 붙여 관리합니다.

### 💡 주요 특징 및 예시
1. **Lucide Icons**: `lucide-react`를 통해 수천 개의 비즈니스 아이콘을 아주 가볍게 불러옵니다.
2. **Scoping**: CSS 이름을 `title`이라고 지어도 다른 파일의 `title`과 충돌하지 않도록 자동으로 독특한 이름을 만들어줍니다.

### 🛠️ 우리 프로젝트에서의 활용
- **Glassmorphism**: 우리 사이트 특유의 투명하고 반짝이는 디자인은 순수 CSS(Vanilla CSS)의 `backdrop-filter` 기술을 사용하여 구현되었습니다.
- **Visual Feedback**: 성공(녹색), 실패(적색) 아이콘을 적재적소에 배치하여 사용자 직관성을 높였습니다.

### 🚀 실전 사용 가이드 (Step-by-Step)
**[아이콘 넣고 스타일 입히기]**
1. **아이콘 선택**: Lucide 사이트에서 원하는 아이콘(예: `Search`)을 찾습니다.
2. **컴포넌트 삽입**: `<Search size={20} />` 처럼 React 코드 안에 넣습니다.
3. **CSS 정의**: `.myIcon { color: blue; }` 처럼 CSS 모듈 파일에 스타일을 적습니다.
4. **클래스 적용**: `<div className={styles.myIcon}>`으로 클래스를 연결합니다.
