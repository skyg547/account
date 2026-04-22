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
  ClipboardCheck
} from 'lucide-react';
import styles from './Sidebar.module.css';

const menuItems = [
  { group: '재무 업무', items: [
    { icon: LayoutDashboard, label: '대시보드', href: '/' },
    { icon: FileText, label: '전표 조회', href: '/journal/list' },
    { icon: Plus, label: '전표 입력', href: '/journal/entry' },
    { icon: BarChart, label: '총계정원장', href: '/journal/ledger/gl' },
    { icon: PieChart, label: '보조원장', href: '/journal/ledger/sl' },
    { icon: Zap, label: '자동 분개 설정', href: '/journal/rules' },
  ]},
  { group: '기준 정보', items: [
    { icon: BookOpen, label: '계정 과목 관리', href: '/master/account' },
    { icon: Users, label: '거래처 관리', href: '/master/partner' },
    { icon: Stamp, label: '승인 관리', href: '/master/approval' },
  ]},
  { group: '결산/보고서', items: [
    { icon: Clock, label: '결산 프로세스', href: '/closing' },
    { icon: ClipboardCheck, label: '재무제표 조회', href: '/reports/statements' },
  ]},
  { group: '시스템 관리', items: [
    { icon: ShieldCheck, label: '사용자 관리', href: '/admin/users' },
    { icon: Server, label: '귀속부서 관리', href: '/admin/department' },
    { icon: History, label: '접속 기록', href: '/admin/logs/access' },
    { icon: Activity, label: '시스템 로그', href: '/admin/logs/system' },
  ]},
];

export default function Sidebar() {
  return (
    <aside className={styles.sidebar}>
      <div className={styles.logo}>
        <div className={styles.logoIcon}>A</div>
        <h1>Account.AI</h1>
      </div>

      <nav className={styles.nav}>
        {menuItems.map((group, gIdx) => (
          <div key={gIdx} className={styles.navGroup}>
            <h3 className={styles.groupLabel}>{group.group}</h3>
            {group.items.map((item, idx) => (
              <a key={idx} href={item.href} className={styles.navItem}>
                <item.icon size={20} />
                <span>{item.label}</span>
              </a>
            ))}
          </div>
        ))}
      </nav>

      <div className={styles.footer}>
        <div className={styles.settings}>
          <Settings size={20} />
          <span>환경 설정</span>
        </div>
      </div>
    </aside>
  );
}
