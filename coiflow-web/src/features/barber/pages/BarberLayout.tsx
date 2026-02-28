import { Outlet } from 'react-router-dom';
import { CalendarDays, Percent, UserRound } from 'lucide-react';
import MainLayout from '@/shared/components/MainLayout';
import type { NavItem } from '@/shared/components/MainLayout';

const NAV: NavItem[] = [
  { label: 'Mon planning', to: '/barber', icon: CalendarDays },
  { label: 'Mes commissions', to: '/barber/commissions', icon: Percent },
  { label: 'Clients', to: '/barber/clients', icon: UserRound },
];

export default function BarberLayout() {
  return (
    <MainLayout title="Espace coiffeur" navItems={NAV}>
      <Outlet />
    </MainLayout>
  );
}
