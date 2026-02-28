import { Outlet } from 'react-router-dom';
import { Store } from 'lucide-react';
import MainLayout from '@/shared/components/MainLayout';
import type { NavItem } from '@/shared/components/MainLayout';

const NAV: NavItem[] = [
  { label: 'Salons', to: '/admin', icon: Store },
];

export default function AdminLayout() {
  return (
    <MainLayout title="Administration" navItems={NAV}>
      <Outlet />
    </MainLayout>
  );
}
