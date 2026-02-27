import { useState, useMemo } from 'react';
import { useAuth } from '@/shared/context/AuthContext';
import { useCommissions } from '@/features/manager/hooks/useCommissions';
import { DataTable } from '@/shared/components/DataTable';
import { formatDateISO, addDays } from '@/shared/utils/dateHelpers';
import { formatPrice } from '@/shared/utils/formatters';
import type { Column } from '@/shared/components/DataTable';
import type { Commission } from '@/shared/types/commission';

const columns: Column<Commission>[] = [
  { header: 'Taux', accessor: (c) => `${Number(c.rateApplied).toFixed(0)} %` },
  { header: 'Montant', accessor: (c) => formatPrice(c.amount) },
  {
    header: 'Date',
    accessor: (c) =>
      c.createdAt ? new Date(c.createdAt).toLocaleDateString('fr-FR') : '',
  },
];

export default function MyCommissions() {
  const { user } = useAuth();
  const today = new Date();
  const [startDate, setStartDate] = useState(() => formatDateISO(addDays(today, -30)));
  const [endDate, setEndDate] = useState(() => formatDateISO(today));

  const { data: commissions = [], isLoading } = useCommissions(startDate, endDate, user?.userId);

  const grandTotal = useMemo(
    () => commissions.reduce((sum, c) => sum + Number(c.amount), 0),
    [commissions],
  );

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-gray-900">Mes commissions</h2>
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
        </div>
      </div>

      <div className="rounded-lg bg-blue-50 p-4 shadow">
        <p className="text-sm font-medium text-blue-600">Total sur la periode</p>
        <p className="mt-1 text-2xl font-semibold text-blue-900">{formatPrice(grandTotal)}</p>
        <p className="text-xs text-blue-400">
          {commissions.length} commission{commissions.length > 1 ? 's' : ''}
        </p>
      </div>

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
