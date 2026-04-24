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
          {/* Main Layout Wrapper */}
          <div className="flex bg-[#020617] min-h-screen selection:bg-blue-500/30 selection:text-blue-200">
            {/* Sidebar - Fixed Left */}
            <Sidebar />

            <div className="flex-1 flex flex-col ml-[300px] min-h-screen">
              {/* Top Global Header - Stays relative to content but fixed top */}
              <TopHeader />
              
              {/* Page Body - Animated transition */}
              <main className="flex-1 p-10 pt-[120px] animate-in fade-in slide-in-from-bottom-8 duration-1000 ease-out">
                {children}
              </main>

              {/* Minimal Footer */}
              <Footer />
            </div>
          </div>
        </NavProvider>
      </body>
    </html>
  );
}
