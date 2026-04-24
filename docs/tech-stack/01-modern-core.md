# 🧊 [Tech 01] Next.js 15 (App Router)

### 👶 초보자를 위한 개념 설명
> **Next.js란 무엇인가요?**
> 마치 **"풀옵션으로 꾸며진 고급 아파트"**와 같습니다. 집(웹사이트)을 지을 때 벽지, 가전, 가구(라우팅, 이미지 최적화, 서버 렌더링 등)를 하나하나 따로 살 필요 없이, 입주하자마자 모든 편의 시설을 바로 사용할 수 있게 해주는 프레임워크입니다.

### 💡 주요 특징 및 예시
1. **App Router**: 폴더 구조가 곧 웹사이트의 주소(URL)가 됩니다.
   - 예: `src/app/finance/budget/page.tsx` 파일은 `http://.../finance/budget` 주소가 됩니다.
2. **Server Components**: 복잡한 계산이나 데이터 조회는 서버에서 미리 처리해서 보내줍니다. 사용자의 브라우저가 훨씬 가벼워집니다.
3. **Optimized Link**: `Link` 컴포넌트를 사용하면 새로고침 없이 아주 빠르게 화면이 전환됩니다.

### 🛠️ 우리 프로젝트에서의 활용
- **Header & Sidebar**: 모든 페이지에서 공통으로 사용되는 레이아웃을 `layout.tsx`에 선언하여 중복 코드를 줄였습니다.
- **Dynamic Navigation**: `NavContext`를 활용하여 상단 메뉴 클릭 시 사이드바가 즉시 바뀌는 반응형 UI를 구현했습니다.

### 🚀 실전 사용 가이드 (Step-by-Step)
**[신규 페이지를 추가하고 싶을 때]**
1. **폴더 생성**: `src/app` 폴더 내부에 주소로 쓰고 싶은 이름의 폴더를 만듭니다 (예: `finance/tax`).
2. **파일 작성**: 해당 폴더에 `page.tsx`라는 이름으로 컴포넌트 코드를 작성합니다.
3. **스타일링**: 같은 폴더에 `[Name].module.css` 파일을 만들고 CSS를 작성한 뒤 `import` 합니다.
4. **확인**: 브라우저에서 `localhost:3000/finance/tax`로 접속하면 화면이 바로 나타납니다.

---

# ☕ [Tech 02] Java 21 (Modern Java)

### 👶 초보자를 위한 개념 설명
> **Java 21은 무엇인가요?**
> 전 세계에서 가장 많이 쓰는 언어인 Java의 **"최신형 스포츠카 버전"**입니다. 이전 세대보다 훨씬 빠르고, 코드가 짧아졌으며, 똑똑해졌습니다.

### 💡 주요 특징 및 예시
1. **Records (데이터 불변성)**: 데이터를 담는 바구니를 아주 쉽게 만듭니다.
   ```java
   public record JournalEntry(String id, BigDecimal amount) {} 
   // 클래스 선언 없이 한 줄로 데이터 객체 정의 끝!
   ```
2. **Pattern Matching**: "만약 이게 사과라면~" 하는 식의 복잡한 조건문을 아주 깔끔하게 바꿉니다.
3. **Virtual Threads**: 아주 적은 메모리로 수백만 개의 작업을 동시에 처리할 수 있게 해줍니다.

### 🚀 실전 사용 가이드 (Step-by-Step)
**[Record를 활용한 데이터 전달 바구니 만들기]**
1. **정의**: 비즈니스 로직 사이에서 주고받을 데이터를 정의합니다.
   ```java
   public record UserProfile(String name, int age) {}
   ```
2. **생성**: `new` 키워드로 데이터를 담습니다.
   ```java
   var user = new UserProfile("홍길동", 28);
   ```
3. **읽기**: 필드명으로 데이터를 꺼냅니다. (게터 메서드가 자동으로 생성됨)
   ```java
   System.out.println(user.name()); // 홍길동 출력
   ```
