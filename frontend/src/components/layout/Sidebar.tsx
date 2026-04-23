"use client";

import React from 'react';
import Link from 'next/link';
import { 
  LayoutDashboard, 
  FileText, 
  BookOpen, 
  Settings, 
  BarChart3, 
  Wallet,
  Activity,
  Plus,
  Users,
  ShieldCheck,
  History,
  Server,
  BarChart,
  PieChart,
  Zap,
  Stamp,
  Clock,
  ClipboardCheck,
  DollarSign,
  CreditCard,
  Percent,
  Package,
  Receipt,
  Landmark,
  Globe,
  TrendingUp,
  CalendarCheck,
  ShieldAlert,
  CalendarDays,
  Network,
  CheckCircle
} from 'lucide-react';
import { useNav, NavCategory } from '@/context/NavContext';
import styles from './Sidebar.module.css';

const menuItems = [
  { 
    category: 'DASHBOARD',
    group: '대시보드', 
    items: [
      { icon: LayoutDashboard, label: '통합 대시보드', href: '/' },
    ]
  },
  { 
    category: 'ACCOUNTING',
    group: '재무 업무', 
    items: [
      { icon: FileText, label: '전표 조회', href: '/journal/list' },
      { icon: Plus, label: '전표 입력', href: '/journal/entry' },
      { icon: BarChart, label: '총계정원장', href: '/journal/ledger/gl' },
      { icon: PieChart, label: '보조원장', href: '/journal/ledger/sl' },
      { icon: Zap, label: '자동 분개 설정', href: '/journal/rules' },
      { icon: CalendarDays, label: '결산 관리', href: '/closing' },
    ]
  },
  { 
    category: 'OPERATIONS',
    group: '재무 운영', 
    items: [
      { icon: DollarSign, label: '매출채권 관리', href: '/finance/receivable' },
      { icon: CreditCard, label: '매입채무 관리', href: '/finance/payable' },
      { icon: Percent, label: '세무/부가세', href: '/finance/tax' },
      { icon: Package, label: '고정자산 관리', href: '/finance/assets' },
      { icon: Receipt, label: '지출결의 포털', href: '/finance/expense' },
    ]
  },
  { 
    category: 'OPERATIONS',
    group: '일반 재무 실무', 
    items: [
      { icon: Wallet, label: '예산 관리', href: '/finance/budget' },
      { icon: Activity, label: '자금 수지 계획', href: '/finance/cashflow' },
      { icon: FileText, label: '리스 회계', href: '/finance/lease' },
      { icon: Network, label: '연결 회계 기초', href: '/finance/consolidation' },
    ]
  },
  { 
    category: 'BANKING',
    group: '은행 특화 업무', 
    items: [
      { icon: Landmark, label: '지점간 정산', href: '/finance/banking/inter-branch' },
      { icon: Globe, label: '외환 포지션', href: '/fx/position' },
      { icon: TrendingUp, label: '내부 금리(FTP)', href: '/finance/banking/ftp' },
      { icon: CalendarCheck, label: '은행 일계표', href: '/finance/banking/daily-summary' },
      { icon: ShieldAlert, label: '감사 모니터링', href: '/finance/banking/audit' },
    ]
  },
  { 
    category: 'RISK',
    group: '리스크 관리', 
    items: [
      { icon: BarChart3, label: 'Basel III RWA 현황', href: '/risk/basel-iii/rwa' },
      { icon: Activity, label: 'IFRS 9 ECL 시뮬레이션', href: '/risk/ifrs-9/ecl' },
      { icon: TrendingUp, label: 'ALM / 금리 리스크', href: '/risk/alm/interest' },
      { icon: ShieldCheck, label: '유동성 리스크(LCR)', href: '/risk/liquidity' },
    ]
  },
  { 
    category: 'MASTER',
    group: '기준 정보', 
    items: [
      { icon: BookOpen, label: '계정 과목 관리', href: '/master/account' },
      { icon: Users, label: '거래처 관리', href: '/master/partner' },
      { icon: CheckCircle, label: '기준 정보 승인', href: '/master/approval' },
    ]
  },
  { 
    category: 'ADMIN',
    group: '시스템 관리', 
    items: [
      { icon: Users, label: '사용자 및 권한', href: '/admin/users' },
      { icon: Landmark, label: '귀속 부서 관리', href: '/admin/dept' },
      { icon: FileText, label: '시스템 로그 조회', href: '/admin/logs' },
    ]
  },
];

export default function Sidebar() {
  const { activeCategory } = useNav();

  const filteredMenuItems = menuItems.filter(group => group.category === activeCategory);

  return (
    <aside className={styles.sidebar}>
      <div className={styles.menuScroll}>
        {filteredMenuItems.map((group, idx) => (
          <div key={idx} className={styles.group}>
            <h3 className={styles.groupTitle}>{group.group}</h3>
            <ul className={styles.itemList}>
              {group.items.map((item, itemIdx) => (
                <li key={itemIdx} className={styles.menuItem}>
                  <a href={item.href} className={styles.link}>
                    <item.icon size={18} className={styles.icon} />
                    <span>{item.label}</span>
                  </a>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>

      <div className={styles.footer}>
        <div className={styles.settings}>
          <Settings size={20} />
          <span>환경 설정</span>
        </div>
      </div>
    </aside>
  );
}
