import { Outlet } from 'react-router-dom';
import MainLayout from '@/shared/components/MainLayout';
import type { NavItem } from '@/shared/components/MainLayout';

const NAV: NavItem[] = [
  { label: 'Tableau de bord', to: '/manager' },
  { label: 'Rendez-vous', to: '/manager/appointments' },
  { label: 'Caisse', to: '/manager/transactions' },
  { label: 'Commissions', to: '/manager/commissions' },
  { label: 'Coiffeurs', to: '/manager/staff' },
  { label: 'Prestations', to: '/manager/services' },
  { label: 'Clients', to: '/manager/clients' },
];

export default function ManagerLayout() {
  return (
    <MainLayout title="Gestion du salon" navItems={NAV}>
      <Outlet />
    </MainLayout>
  );
}
