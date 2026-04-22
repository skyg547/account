import type { Metadata } from "next";
import { Inter } from "next/font/google";
import Sidebar from "@/components/layout/Sidebar";
import Navbar from "@/components/layout/Navbar";
import "./globals.css";

/**
 * [폰트 설정]
 * 'Inter'라는 예쁜 영문 폰트를 불러옵니다. 
 * 가독성이 좋아서 전 세계적으로 많이 쓰이는 폰트예요.
 */
const inter = Inter({ subsets: ["latin"] });

/**
 * [SEO 및 탭 이름 설정]
 * 브라우저 탭에 보일 제목(title)과 
 * 검색 엔진(Google 등)이 읽어갈 설명(description)을 적는 곳입니다.
 */
export const metadata: Metadata = {
  title: "Account.AI | 모던 재무 관리 시스템",
  description: "MSA 기반의 차세대 지능형 재무 및 회계 관리 플랫폼",
};

/**
 * [루트 레이아웃]
 * 우리 웹사이트의 '가장 바깥쪽 틀'입니다.
 * 모든 페이지는 이 틀 안에서 보여지게 됩니다. (마치 티비 프레임처럼요!)
 */
export default function RootLayout({
  children, // 실제 각 페이지의 내용물이 이 자리(children)에 쏙 들어옵니다.
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body className={inter.className}>
        {/* 전체 앱을 감싸는 전체 바구니 */}
        <div className="app-container">
          
          {/* 1. 좌측 메뉴판 (사이드바) */}
          <Sidebar />

          {/* 2. 오른쪽 메인 내용 영역 */}
          <div className="main-wrapper">
            
            {/* 2-1. 상단 알림 및 검색 바 (네비바) */}
            <Navbar />

            {/* 2-2. 실제 우리가 코딩할 페이지 본문 */}
            <main className="content">
              {children}
            </main>

          </div>
        </div>
      </body>
    </html>
  );
}
