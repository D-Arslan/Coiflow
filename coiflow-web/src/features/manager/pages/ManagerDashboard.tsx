import { useMemo } from 'react';
import { DollarSign, CalendarCheck, Clock, UsersRound } from 'lucide-react';
import { useDashboardStats, useDashboardRevenue } from '@/features/manager/hooks/useDashboard';
import { formatDateISO, addDays } from '@/shared/utils/dateHelpers';
import { formatPrice } from '@/shared/utils/formatters';

const STATUS_LABELS: Record<string, string> = {
  SCHEDULED: 'Planifies',
  IN_PROGRESS: 'En cours',
  COMPLETED: 'Termines',
  CANCELLED: 'Annules',
  NO_SHOW: 'Absents',
};

const STATUS_COLORS: Record<string, string> = {
  SCHEDULED: 'bg-amber-100 text-amber-800',
  IN_PROGRESS: 'bg-sky-100 text-sky-800',
  COMPLETED: 'bg-emerald-100 text-emerald-800',
  CANCELLED: 'bg-red-100 text-red-800',
  NO_SHOW: 'bg-stone-200 text-stone-700',
};

export default function ManagerDashboard() {
  const { data: stats, isLoading: statsLoading } = useDashboardStats();

  const today = useMemo(() => new Date(), []);
  const startStr = useMemo(() => formatDateISO(addDays(today, -6)), [today]);
  const endStr = useMemo(() => formatDateISO(today), [today]);
  const { data: revenue = [], isLoading: revenueLoading } = useDashboardRevenue(startStr, endStr);

  if (statsLoading) {
    return (
      <div className="flex justify-center py-12">
        <div className="h-8 w-8 animate-spin rounded-full border-4 border-amber-500 border-t-transparent" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-stone-800">Tableau de bord</h2>

      {/* Stat cards */}
      <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
        <div className="rounded-lg bg-white p-6 shadow">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-stone-500">CA du jour</h3>
            <DollarSign className="h-5 w-5 text-amber-500" />
          </div>
          <p className="mt-2 text-3xl font-bold text-stone-800">
            {formatPrice(stats?.revenueToday ?? 0)}
          </p>
        </div>
        <div className="rounded-lg bg-white p-6 shadow">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-stone-500">RDV aujourd'hui</h3>
            <CalendarCheck className="h-5 w-5 text-amber-500" />
          </div>
          <p className="mt-2 text-3xl font-bold text-stone-800">
            {stats?.appointmentsToday ?? 0}
          </p>
          <div className="mt-3 flex flex-wrap gap-1">
            {Object.entries(stats?.appointmentsByStatus ?? {})
              .filter(([, count]) => count > 0)
              .map(([status, count]) => (
                <span
                  key={status}
                  className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ${STATUS_COLORS[status] ?? 'bg-stone-100 text-stone-700'}`}
                >
                  {count} {STATUS_LABELS[status] ?? status}
                </span>
              ))}
          </div>
        </div>
        <div className="rounded-lg bg-white p-6 shadow">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-stone-500">RDV planifies</h3>
            <Clock className="h-5 w-5 text-amber-500" />
          </div>
          <p className="mt-2 text-3xl font-bold text-stone-800">
            {stats?.appointmentsByStatus?.SCHEDULED ?? 0}
          </p>
        </div>
        <div className="rounded-lg bg-white p-6 shadow">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-medium text-stone-500">Coiffeurs actifs</h3>
            <UsersRound className="h-5 w-5 text-amber-500" />
          </div>
          <p className="mt-2 text-3xl font-bold text-stone-800">
            {stats?.activeBarbersCount ?? 0}
          </p>
        </div>
      </div>

      {/* Revenue table - last 7 days */}
      <div className="rounded-lg bg-white shadow">
        <div className="border-b border-stone-200 px-6 py-4">
          <h3 className="text-sm font-medium text-stone-800">Chiffre d'affaires - 7 derniers jours</h3>
        </div>
        {revenueLoading ? (
          <div className="flex justify-center py-8">
            <div className="h-6 w-6 animate-spin rounded-full border-4 border-amber-500 border-t-transparent" />
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b bg-stone-50 text-left text-stone-500">
                <th className="px-6 py-3 font-medium">Date</th>
                <th className="px-6 py-3 font-medium text-right">CA</th>
                <th className="px-6 py-3 font-medium text-right">Transactions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-stone-200">
              {revenue.map((r) => (
                <tr key={r.date} className="hover:bg-stone-50 transition-colors">
                  <td className="px-6 py-3">
                    {new Date(r.date + 'T00:00:00').toLocaleDateString('fr-FR', {
                      weekday: 'short',
                      day: 'numeric',
                      month: 'short',
                    })}
                  </td>
                  <td className="px-6 py-3 text-right font-medium">{formatPrice(r.revenue)}</td>
                  <td className="px-6 py-3 text-right">{r.transactionCount}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
