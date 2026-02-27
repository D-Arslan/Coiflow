import { Outlet } from 'react-router-dom';
import MainLayout from '@/shared/components/MainLayout';
import type { NavItem } from '@/shared/components/MainLayout';

const NAV: NavItem[] = [
  { label: 'Mon planning', to: '/barber' },
  { label: 'Mes commissions', to: '/barber/commissions' },
  { label: 'Clients', to: '/barber/clients' },
];

export default function BarberLayout() {
  return (
    <MainLayout title="Espace coiffeur" navItems={NAV}>
      <Outlet />
    </MainLayout>
  );
}
