import { useState, useMemo } from 'react';
import { useCommissions } from '@/features/manager/hooks/useCommissions';
import { useStaff } from '@/features/manager/hooks/useStaff';
import { DataTable } from '@/shared/components/DataTable';
import { formatDateISO, addDays } from '@/shared/utils/dateHelpers';
import { formatPrice } from '@/shared/utils/formatters';
import type { Column } from '@/shared/components/DataTable';
import type { Commission } from '@/shared/types/commission';

const columns: Column<Commission>[] = [
  { header: 'Coiffeur', accessor: 'barberName' },
  { header: 'Taux', accessor: (c) => `${Number(c.rateApplied).toFixed(0)} %` },
  { header: 'Montant', accessor: (c) => formatPrice(c.amount) },
  { header: 'Date', accessor: (c) => c.createdAt ? new Date(c.createdAt).toLocaleDateString('fr-FR') : '' },
];

interface BarberTotal {
  name: string;
  total: number;
  count: number;
}

export default function CommissionsPage() {
  const today = new Date();
  const [startDate, setStartDate] = useState(() => formatDateISO(addDays(today, -30)));
  const [endDate, setEndDate] = useState(() => formatDateISO(today));
  const [barberFilter, setBarberFilter] = useState('');

  const { data: commissions = [], isLoading } = useCommissions(startDate, endDate, barberFilter || undefined);
  const { data: staff = [] } = useStaff();

  // Totals by barber
  const barberTotals = useMemo(() => {
    const map = new Map<string, BarberTotal>();
    for (const c of commissions) {
      const existing = map.get(c.barberId);
      if (existing) {
        existing.total += Number(c.amount);
        existing.count += 1;
      } else {
        map.set(c.barberId, { name: c.barberName, total: Number(c.amount), count: 1 });
      }
    }
    return Array.from(map.values()).sort((a, b) => b.total - a.total);
  }, [commissions]);

  const grandTotal = useMemo(
    () => commissions.reduce((sum, c) => sum + Number(c.amount), 0),
    [commissions],
  );

  return (
    <div className="space-y-6">
      {/* Header + Filters */}
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-gray-900">Commissions</h2>
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <span className="text-sm text-gray-500">au</span>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          />
          <select
            value={barberFilter}
            onChange={(e) => setBarberFilter(e.target.value)}
            className="rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
          >
            <option value="">Tous les coiffeurs</option>
            {staff.map((s) => (
              <option key={s.id} value={s.id}>
                {s.firstName} {s.lastName}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Summary cards */}
      {barberTotals.length > 0 && (
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
          {barberTotals.map((bt) => (
            <div key={bt.name} className="rounded-lg bg-white p-4 shadow">
              <p className="text-sm font-medium text-gray-500">{bt.name}</p>
              <p className="mt-1 text-lg font-semibold text-gray-900">{formatPrice(bt.total)}</p>
              <p className="text-xs text-gray-400">{bt.count} transaction{bt.count > 1 ? 's' : ''}</p>
            </div>
          ))}
          <div className="rounded-lg bg-blue-50 p-4 shadow">
            <p className="text-sm font-medium text-blue-600">Total general</p>
            <p className="mt-1 text-lg font-semibold text-blue-900">{formatPrice(grandTotal)}</p>
            <p className="text-xs text-blue-400">{commissions.length} commission{commissions.length > 1 ? 's' : ''}</p>
          </div>
        </div>
      )}

      {/* Table */}
      <DataTable<Commission>
        columns={columns}
        data={commissions}
        keyExtractor={(c) => c.id}
        isLoading={isLoading}
        emptyMessage="Aucune commission sur cette periode"
      />
    </div>
  );
}
