import React from 'react';
import { Bell, Search, User, Moon, Sun } from 'lucide-react';
import styles from './Navbar.module.css';

export default function Navbar() {
  return (
    <header className="h-[80px] bg-slate-950/50 backdrop-blur-xl border-b border-white/5 flex items-center justify-between px-10 sticky top-0 z-[90] transition-all duration-500">
      <div className="flex items-center gap-4 group">
        <div className="relative w-[480px]">
          <Search size={18} className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500 group-hover:text-blue-400 transition-colors" />
          <input 
            type="text" 
            placeholder="Search Intelligence (Journals, Ledger, Partners...)" 
            className="w-full bg-white/[0.03] border border-white/5 rounded-2xl py-3 pl-12 pr-4 text-sm text-slate-300 placeholder:text-slate-600 outline-none focus:bg-white/5 focus:border-blue-500/50 transition-all shadow-inner"
          />
        </div>
      </div>
      
      <div className="flex items-center gap-8">
        <div className="flex items-center gap-2">
          <button className="w-12 h-12 rounded-2xl flex items-center justify-center text-slate-400 hover:text-white hover:bg-white/5 transition-all relative group">
            <Moon size={20} className="group-hover:rotate-[15deg] transition-transform" />
          </button>
          <button className="w-12 h-12 rounded-2xl flex items-center justify-center text-slate-400 hover:text-blue-400 hover:bg-blue-500/10 transition-all relative group">
            <Bell size={20} className="group-hover:animate-bounce" />
            <span className="absolute top-3 right-3 w-2.5 h-2.5 bg-rose-500 rounded-full border-2 border-slate-950 shadow-[0_0_10px_rgba(244,63,94,0.5)]" />
          </button>
        </div>

        <div className="h-8 w-px bg-white/10 mx-2" />

        <div className="flex items-center gap-4 pl-2 group cursor-pointer">
          <div className="relative">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-white shadow-lg shadow-blue-500/20 group-hover:scale-105 transition-transform duration-300 relative z-10 overflow-hidden">
               <User size={24} />
               <div className="absolute inset-0 bg-white/20 opacity-0 group-hover:opacity-100 transition-opacity" />
            </div>
            <div className="absolute -inset-1 bg-blue-500/20 blur opacity-0 group-hover:opacity-100 transition-opacity rounded-2xl" />
          </div>
          <div className="flex flex-col">
            <span className="text-sm font-black text-white tracking-tight leading-none group-hover:text-blue-400 transition-colors">김재무 팀장</span>
            <span className="text-[10px] font-black text-slate-500 uppercase tracking-widest mt-1 italic">Financial Controller</span>
          </div>
        </div>
      </div>
    </header>
  );
}
