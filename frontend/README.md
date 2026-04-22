# 🎨 Account.AI Frontend (Modern Financial System UI)

본 모듈은 MSA 기반 재무 시스템의 사용자 인터페이스를 담당하는 **Next.js 15** 애플리케이션입니다. 
초보자분들도 쉽게 적응할 수 있도록 설계되었으니, 아래 가이드를 천천히 따라와 주세요!

---

## 📚 초보자를 위한 학습 & 개발 가이드
처음 오셨다면 아래 문서들을 순서대로 읽어보시는 것을 강력 추천합니다.

1.  [**🐣 프론트엔드 입문 가이드**](./docs/beginner-guide.md): 우리 프로젝트의 구조와 기초 개념을 설명합니다.
2.  [**🖥️ 화면 목록 및 기능 명세**](./docs/screen-inventory.md): 현재 구축된 모든 화면과 주요 기능을 한눈에 확인합니다.
3.  [**🛠️ 실전 개발 가이드**](./docs/development-guide.md): 페이지를 만들고 디자인을 입히는 구체적인 방법을 알려줍니다.
4.  [**🚒 런북 (장해 해결)**](./docs/runbook.md): 에러가 났을 때 당황하지 않고 대처하는 법을 모았습니다.

---

## 🚀 빠른 시작 (Running Locally)

### 💻 내 컴퓨터에서 바로 실행하기
```bash
# 1. 프론트엔드 폴더로 이동
cd frontend

# 2. 필요한 도구들(패키지) 설치
npm install

# 3. 개발 서버 실행
npm run dev
```
이제 브라우저에서 [http://localhost:3000](http://localhost:3000)을 열어보세요!

### 🐳 Docker로 간편하게 실행하기 (Docker 필요)
설사 내 컴퓨터에 Node.js가 없어도 도커만 있다면 실행할 수 있습니다.
```bash
# 1. 이미지 빌드 및 실행
docker-compose up --build
```
이제 [http://localhost:4000](http://localhost:4000)에서 결과를 확인할 수 있습니다.

---

## 🏗️ 우리의 기술 약속 (Tech Stack)

*   **뼈대:** Next.js 15 (최신 App Router 방식)
*   **언어:** TypeScript (실수를 줄여주는 꼼꼼한 조수)
*   **디자인:** Vanilla CSS + CSS Modules (우리만의 독창적인 스타일)
*   **테마:** **글래스모피즘(Glassmorphism)** - 유리처럼 비치는 고급스러운 디자인

---

## 📂 폴더 구조 퀵뷰
*   `src/app`: 웹사이트의 주소와 페이지 내용 (`Page`, `Layout`)
*   `src/components`: 반복해서 재사용할 수 있는 예쁜 부품들
*   `src/styles`: 전역 디자인 규칙 (`globals.css`)
*   `docs`: 여러분을 위한 상세 가이드 문서함

---
**담당자: [프론트]**
*마지막 수정: 2026-04-22*
