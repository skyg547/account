import { UserRole } from '@/context/NavContext';

export interface MenuItem {
  icon: string;  // lucide-react icon name
  label: string;
  href: string;
  requiredRoles?: UserRole[];
}

export interface MenuGroup {
  category: string;
  group: string;
  module: string;  // backend module name for traceability
  requiredRoles?: UserRole[];
  items: MenuItem[];
}
