"use client";

import React from 'react';
import Sidebar from "./Sidebar";
import TopHeader from "./TopHeader";
import Footer from "./Footer";
import { useNav } from "@/context/NavContext";

/**
 * [메인 레이아웃 래퍼]
 * 사이드바 상태(접힘 여부)에 따라 메인 콘텐츠의 여백을 동적으로 조절합니다.
 */
export default function MainLayout({ children }: { children: React.ReactNode }) {
  const { isCollapsed } = useNav();

  return (
    <div className="flex bg-[#020617] min-h-screen selection:bg-blue-500/30 selection:text-blue-200 overflow-x-hidden">
      {/* Sidebar - Fixed Left */}
      <Sidebar />

      <div className={`flex-1 flex flex-col ${isCollapsed ? 'ml-20' : 'ml-[300px]'} min-h-screen transition-all duration-500 relative`}>
        {/* Top Global Header */}
        <TopHeader />
        
        {/* Page Body */}
        <main className="flex-1 p-10 pt-[120px] animate-in fade-in slide-in-from-bottom-4 duration-1000 ease-out relative">
          <div className="max-w-[1600px] mx-auto">
            {children}
          </div>
        </main>

        {/* Minimal Footer */}
        <Footer />
      </div>
    </div>
  );
}
