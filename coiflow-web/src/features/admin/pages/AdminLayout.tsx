import { Outlet } from 'react-router-dom';
import MainLayout from '@/shared/components/MainLayout';
import type { NavItem } from '@/shared/components/MainLayout';

const NAV: NavItem[] = [
  { label: 'Salons', to: '/admin' },
];

export default function AdminLayout() {
  return (
    <MainLayout title="Administration" navItems={NAV}>
      <Outlet />
    </MainLayout>
  );
}
