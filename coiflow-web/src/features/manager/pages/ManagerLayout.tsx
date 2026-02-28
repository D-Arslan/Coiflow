import { Outlet } from 'react-router-dom';
import { LayoutDashboard, Calendar, Receipt, Percent, Users, Scissors, UserRound } from 'lucide-react';
import MainLayout from '@/shared/components/MainLayout';
import type { NavItem } from '@/shared/components/MainLayout';

const NAV: NavItem[] = [
  { label: 'Tableau de bord', to: '/manager', icon: LayoutDashboard },
  { label: 'Rendez-vous', to: '/manager/appointments', icon: Calendar },
  { label: 'Caisse', to: '/manager/transactions', icon: Receipt },
  { label: 'Commissions', to: '/manager/commissions', icon: Percent },
  { label: 'Coiffeurs', to: '/manager/staff', icon: Users },
  { label: 'Prestations', to: '/manager/services', icon: Scissors },
  { label: 'Clients', to: '/manager/clients', icon: UserRound },
];

export default function ManagerLayout() {
  return (
    <MainLayout title="Gestion du salon" navItems={NAV}>
      <Outlet />
    </MainLayout>
  );
}
