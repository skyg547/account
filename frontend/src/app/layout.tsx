import type { Metadata } from "next";
import { Inter } from "next/font/google";
import Sidebar from "@/components/layout/Sidebar";
import TopHeader from "@/components/layout/TopHeader";
import Footer from "@/components/layout/Footer";
import { NavProvider } from "@/context/NavContext";
import "./globals.css";

const inter = Inter({ subsets: ["latin"] });

/**
 * [SEO 및 탭 이름 설정]
 */
export const metadata: Metadata = {
  title: "Account.AI | 모던 재무 관리 시스템",
  description: "MSA 기반의 차세대 지능형 재무 및 회계 관리 플랫폼",
};

/**
 * [루트 레이아웃]
 */
export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body className={inter.className}>
        <NavProvider>
          {/* 1. 최상단 고정 헤더 (시스템명, 사용자정보) */}
          <TopHeader />

          <div className="app-container">
            {/* 2. 좌측 메뉴판 (사이드바) */}
            <Sidebar />

            {/* 3. 오른쪽 메인 내용 영역 */}
            <div className="main-wrapper">
              
              {/* 3-1. 실제 페이지 본문 */}
              <main className="content">
                {children}
              </main>

              {/* 3-2. 하단 푸터 (환경 정보 표시) */}
              <Footer />

            </div>
          </div>
        </NavProvider>
      </body>
    </html>
  );
}
