import type { Metadata } from "next";
import MainLayout from "@/components/layout/MainLayout";
import { NavProvider } from "@/context/NavContext";
import { ThemeProvider } from "@/context/ThemeContext";
import "./globals.css";

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
    <html lang="ko" suppressHydrationWarning>
      <body>
        <ThemeProvider>
          <NavProvider>
            <MainLayout>
              {children}
            </MainLayout>
          </NavProvider>
        </ThemeProvider>
      </body>
    </html>
  );
}
