import { useState, useMemo } from 'react';
import { useAppointmentsToCash } from '@/features/manager/hooks/useAppointments';
import { useTransactions, useCreateTransaction, useVoidTransaction } from '@/features/manager/hooks/useTransactions';
import { DataTable } from '@/shared/components/DataTable';
import { Modal } from '@/shared/components/Modal';
import { formatDateISO, addDays } from '@/shared/utils/dateHelpers';
import { formatPrice, formatDuration } from '@/shared/utils/formatters';
import type { Column } from '@/shared/components/DataTable';
import type { Appointment } from '@/shared/types/appointment';
import type { Transaction, PaymentMethod, PaymentLinePayload } from '@/shared/types/transaction';

const PAYMENT_METHODS: { value: PaymentMethod; label: string }[] = [
  { value: 'CASH', label: 'Especes' },
  { value: 'CARD', label: 'Carte' },
  { value: 'CHECK', label: 'Cheque' },
  { value: 'TRANSFER', label: 'Virement' },
  { value: 'OTHER', label: 'Autre' },
];

const toCashColumns: Column<Appointment>[] = [
  { header: 'Coiffeur', accessor: 'barberName' },
  { header: 'Client', accessor: (a) => a.clientName ?? 'Sans client' },
  {
    header: 'Prestations',
    accessor: (a) => a.services.map((s) => s.serviceName).join(', '),
  },
  {
    header: 'Total',
    accessor: (a) => formatPrice(a.totalPrice),
  },
];

const txColumns: Column<Transaction>[] = [
  { header: 'Coiffeur', accessor: 'barberName' },
  { header: 'Total', accessor: (t) => formatPrice(t.totalAmount) },
  { header: 'Paiements', accessor: (t) => t.payments.map((p) => `${p.method} ${formatPrice(p.amount)}`).join(' + ') },
  { header: 'Statut', accessor: 'status' },
  { header: 'Par', accessor: 'createdBy' },
];

export default function TransactionsPage() {
  const today = new Date();
  const [startDate, setStartDate] = useState(() => formatDateISO(addDays(today, -30)));
  const [endDate, setEndDate] = useState(() => formatDateISO(today));

  // To-cash appointments
  const { data: toCash = [], isLoading: toCashLoading } = useAppointmentsToCash(startDate, endDate);
  // Transaction history
  const { data: transactions = [], isLoading: txLoading } = useTransactions(startDate, endDate);

  const createMutation = useCreateTransaction();
  const voidMutation = useVoidTransaction();

  // Cash modal state
  const [cashingAppointment, setCashingAppointment] = useState<Appointment | null>(null);
  const [paymentLines, setPaymentLines] = useState<PaymentLinePayload[]>([{ method: 'CASH', amount: 0 }]);

  const openCashModal = (appointment: Appointment) => {
    setCashingAppointment(appointment);
    setPaymentLines([{ method: 'CASH', amount: appointment.totalPrice }]);
  };

  const addPaymentLine = () => {
    setPaymentLines((prev) => [...prev, { method: 'CASH', amount: 0 }]);
  };

  const removePaymentLine = (index: number) => {
    setPaymentLines((prev) => prev.filter((_, i) => i !== index));
  };

  const updatePaymentLine = (index: number, field: 'method' | 'amount', value: string) => {
    setPaymentLines((prev) =>
      prev.map((line, i) =>
        i === index
          ? { ...line, [field]: field === 'amount' ? Number(value) : value }
          : line,
      ),
    );
  };

  const paymentSum = useMemo(
    () => paymentLines.reduce((sum, l) => sum + (l.amount || 0), 0),
    [paymentLines],
  );

  const isPaymentValid = cashingAppointment
    ? Math.abs(paymentSum - cashingAppointment.totalPrice) < 0.01
    : false;

  const handleCash = () => {
    if (!cashingAppointment || !isPaymentValid) return;
    createMutation.mutate(
      {
        appointmentId: cashingAppointment.id,
        payments: paymentLines.map((l) => ({
          method: l.method as PaymentMethod,
          amount: l.amount,
        })),
      },
      {
        onSuccess: () => {
          setCashingAppointment(null);
          setPaymentLines([{ method: 'CASH', amount: 0 }]);
        },
      },
    );
  };

  return (
    <div className="space-y-8">
      {/* Date range filter */}
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-stone-800">Caisse & Transactions</h2>
        <div className="flex items-center gap-2">
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="rounded-md border border-stone-300 px-3 py-1.5 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
          />
          <span className="text-sm text-stone-500">au</span>
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="rounded-md border border-stone-300 px-3 py-1.5 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
          />
        </div>
      </div>

      {/* Section: A encaisser */}
      <div className="space-y-3">
        <h3 className="text-lg font-medium text-stone-700">A encaisser</h3>
        <DataTable<Appointment>
          columns={toCashColumns}
          data={toCash}
          keyExtractor={(a) => a.id}
          isLoading={toCashLoading}
          emptyMessage="Aucun rendez-vous a encaisser"
          actions={(a) => (
            <button
              onClick={() => openCashModal(a)}
              className="rounded-md bg-green-600 px-3 py-1 text-sm font-medium text-white hover:bg-green-700"
            >
              Encaisser
            </button>
          )}
        />
      </div>

      {/* Section: Historique transactions */}
      <div className="space-y-3">
        <h3 className="text-lg font-medium text-stone-700">Historique</h3>
        <DataTable<Transaction>
          columns={txColumns}
          data={transactions}
          keyExtractor={(t) => t.id}
          isLoading={txLoading}
          emptyMessage="Aucune transaction"
          actions={(t) =>
            t.status === 'COMPLETED' ? (
              <button
                onClick={() => {
                  if (confirm('Annuler cette transaction ?')) voidMutation.mutate(t.id);
                }}
                disabled={voidMutation.isPending}
                className="text-red-600 hover:text-red-800 text-sm font-medium"
              >
                Annuler
              </button>
            ) : (
              <span className="text-sm text-stone-400">Annulee</span>
            )
          }
        />
      </div>

      {/* Cash Modal */}
      <Modal
        isOpen={cashingAppointment !== null}
        onClose={() => setCashingAppointment(null)}
        title="Encaisser le rendez-vous"
      >
        {cashingAppointment && (
          <div className="space-y-4">
            {/* Recap */}
            <div className="rounded-md bg-stone-50 p-3 text-sm">
              <p><span className="font-medium">Coiffeur :</span> {cashingAppointment.barberName}</p>
              <p><span className="font-medium">Client :</span> {cashingAppointment.clientName ?? 'Sans client'}</p>
              <ul className="mt-2 space-y-0.5">
                {cashingAppointment.services.map((s) => (
                  <li key={s.serviceId} className="flex justify-between">
                    <span>{s.serviceName} ({formatDuration(s.durationMinutes)})</span>
                    <span>{formatPrice(s.priceApplied)}</span>
                  </li>
                ))}
              </ul>
              <div className="mt-2 border-t border-stone-200 pt-2 font-semibold flex justify-between">
                <span>Total</span>
                <span>{formatPrice(cashingAppointment.totalPrice)}</span>
              </div>
            </div>

            {/* Payment lines */}
            <div className="space-y-2">
              <label className="block text-sm font-medium text-stone-700">Paiements</label>
              {paymentLines.map((line, i) => (
                <div key={i} className="flex items-center gap-2">
                  <select
                    value={line.method}
                    onChange={(e) => updatePaymentLine(i, 'method', e.target.value)}
                    className="rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  >
                    {PAYMENT_METHODS.map((m) => (
                      <option key={m.value} value={m.value}>
                        {m.label}
                      </option>
                    ))}
                  </select>
                  <input
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={line.amount || ''}
                    onChange={(e) => updatePaymentLine(i, 'amount', e.target.value)}
                    placeholder="Montant"
                    className="flex-1 rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                  />
                  {paymentLines.length > 1 && (
                    <button
                      type="button"
                      onClick={() => removePaymentLine(i)}
                      className="text-red-500 hover:text-red-700 text-sm"
                    >
                      Supprimer
                    </button>
                  )}
                </div>
              ))}
              <button
                type="button"
                onClick={addPaymentLine}
                className="text-sm text-amber-600 hover:text-amber-700 font-medium"
              >
                + Ajouter un moyen de paiement
              </button>
            </div>

            {/* Sum validation */}
            <div className={`text-sm font-medium ${isPaymentValid ? 'text-green-600' : 'text-red-600'}`}>
              Total paiements : {formatPrice(paymentSum)}
              {!isPaymentValid && ` (attendu : ${formatPrice(cashingAppointment.totalPrice)})`}
            </div>

            {/* Buttons */}
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setCashingAppointment(null)}
                className="rounded-md border border-stone-300 px-4 py-2 text-sm text-stone-700 hover:bg-stone-50"
              >
                Annuler
              </button>
              <button
                onClick={handleCash}
                disabled={!isPaymentValid || createMutation.isPending}
                className="rounded-md bg-green-600 px-4 py-2 text-sm font-medium text-white hover:bg-green-700 disabled:opacity-50"
              >
                {createMutation.isPending ? 'Encaissement...' : 'Encaisser'}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
