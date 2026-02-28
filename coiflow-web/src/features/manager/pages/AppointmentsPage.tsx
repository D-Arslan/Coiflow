import { useState, useMemo, type FormEvent } from 'react';
import { useAppointments, useCreateAppointment, useUpdateStatus, useRescheduleAppointment } from '@/features/manager/hooks/useAppointments';
import { useStaff } from '@/features/manager/hooks/useStaff';
import { useServices } from '@/features/manager/hooks/useServices';
import { useClients } from '@/features/manager/hooks/useClients';
import { WeekCalendar } from '@/shared/components/WeekCalendar';
import { Modal } from '@/shared/components/Modal';
import { getWeekRange, addDays, formatDateISO, formatTime } from '@/shared/utils/dateHelpers';
import { formatPrice, formatDuration } from '@/shared/utils/formatters';
import type { Appointment, AppointmentStatus, CreateAppointmentPayload } from '@/shared/types/appointment';

const STATUS_LABELS: Record<AppointmentStatus, string> = {
  SCHEDULED: 'Planifie',
  IN_PROGRESS: 'En cours',
  COMPLETED: 'Termine',
  CANCELLED: 'Annule',
  NO_SHOW: 'Absent',
};

const TRANSITIONS: Record<AppointmentStatus, AppointmentStatus[]> = {
  SCHEDULED: ['IN_PROGRESS', 'CANCELLED', 'NO_SHOW'],
  IN_PROGRESS: ['COMPLETED', 'CANCELLED'],
  COMPLETED: [],
  CANCELLED: [],
  NO_SHOW: [],
};

export default function AppointmentsPage() {
  const [currentWeek, setCurrentWeek] = useState(() => new Date());
  const [barberFilter, setBarberFilter] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);
  const [createDefaults, setCreateDefaults] = useState<{ date: string; time: string }>({ date: '', time: '' });
  const [isRescheduleOpen, setIsRescheduleOpen] = useState(false);

  // Computed week range
  const { start, end } = useMemo(() => getWeekRange(currentWeek), [currentWeek]);
  const weekDays = useMemo(() => {
    const days: Date[] = [];
    for (let i = 0; i < 7; i++) days.push(addDays(start, i));
    return days;
  }, [start]);

  const startStr = formatDateISO(start);
  const endStr = formatDateISO(end);

  // Data hooks
  const { data: appointments = [], isLoading } = useAppointments(startStr, endStr, barberFilter || undefined);
  const { data: staff = [] } = useStaff();
  const { data: services = [] } = useServices();

  // Client search for create modal
  const [clientSearch, setClientSearch] = useState('');
  const { data: clients = [] } = useClients(clientSearch);

  // Service selection for create modal
  const [selectedServiceIds, setSelectedServiceIds] = useState<string[]>([]);

  const createMutation = useCreateAppointment();
  const statusMutation = useUpdateStatus();
  const rescheduleMutation = useRescheduleAppointment();

  const toggleService = (id: string) => {
    setSelectedServiceIds((prev) =>
      prev.includes(id) ? prev.filter((s) => s !== id) : [...prev, id],
    );
  };

  const selectedServicesTotal = useMemo(() => {
    return services
      .filter((s) => selectedServiceIds.includes(s.id))
      .reduce((sum, s) => sum + Number(s.price), 0);
  }, [services, selectedServiceIds]);

  const selectedServicesDuration = useMemo(() => {
    return services
      .filter((s) => selectedServiceIds.includes(s.id))
      .reduce((sum, s) => sum + s.durationMinutes, 0);
  }, [services, selectedServiceIds]);

  const handleSlotClick = (day: Date, hour: number, minute: number) => {
    const date = formatDateISO(day);
    const time = `${String(hour).padStart(2, '0')}:${String(minute).padStart(2, '0')}`;
    setCreateDefaults({ date, time });
    setSelectedServiceIds([]);
    setClientSearch('');
    setIsCreateOpen(true);
  };

  const handleCreate = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    const form = new FormData(e.currentTarget);
    const date = form.get('date') as string;
    const time = form.get('time') as string;
    const payload: CreateAppointmentPayload = {
      barberId: form.get('barberId') as string,
      startTime: `${date}T${time}:00`,
      serviceIds: selectedServiceIds,
    };
    const clientId = form.get('clientId') as string;
    if (clientId) payload.clientId = clientId;
    const notes = form.get('notes') as string;
    if (notes) payload.notes = notes;

    createMutation.mutate(payload, {
      onSuccess: () => {
        setIsCreateOpen(false);
        setSelectedServiceIds([]);
        setClientSearch('');
      },
    });
  };

  const handleStatusChange = (status: AppointmentStatus) => {
    if (!selectedAppointment) return;
    statusMutation.mutate(
      { id: selectedAppointment.id, status },
      { onSuccess: () => setSelectedAppointment(null) },
    );
  };

  const handleReschedule = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedAppointment) return;
    const form = new FormData(e.currentTarget);
    const date = form.get('rescheduleDate') as string;
    const time = form.get('rescheduleTime') as string;
    const barberId = form.get('rescheduleBarberId') as string;
    rescheduleMutation.mutate(
      {
        id: selectedAppointment.id,
        payload: {
          startTime: `${date}T${time}:00`,
          ...(barberId !== selectedAppointment.barberId ? { barberId } : {}),
        },
      },
      {
        onSuccess: () => {
          setIsRescheduleOpen(false);
          setSelectedAppointment(null);
        },
      },
    );
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h2 className="text-xl font-semibold text-stone-800">Rendez-vous</h2>
        <button
          onClick={() => {
            setCreateDefaults({ date: formatDateISO(new Date()), time: '09:00' });
            setSelectedServiceIds([]);
            setClientSearch('');
            setIsCreateOpen(true);
          }}
          className="rounded-md bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700"
        >
          Nouveau RDV
        </button>
      </div>

      {/* Week nav + barber filter */}
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <button
            onClick={() => setCurrentWeek((w) => addDays(w, -7))}
            className="rounded-md border border-stone-300 px-3 py-1.5 text-sm text-stone-700 hover:bg-stone-50"
          >
            &larr; Semaine prec.
          </button>
          <button
            onClick={() => setCurrentWeek(new Date())}
            className="rounded-md border border-stone-300 px-3 py-1.5 text-sm text-stone-700 hover:bg-stone-50"
          >
            Aujourd'hui
          </button>
          <button
            onClick={() => setCurrentWeek((w) => addDays(w, 7))}
            className="rounded-md border border-stone-300 px-3 py-1.5 text-sm text-stone-700 hover:bg-stone-50"
          >
            Semaine suiv. &rarr;
          </button>
        </div>
        <select
          value={barberFilter}
          onChange={(e) => setBarberFilter(e.target.value)}
          className="rounded-md border border-stone-300 px-3 py-1.5 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
        >
          <option value="">Tous les coiffeurs</option>
          {staff.map((s) => (
            <option key={s.id} value={s.id}>
              {s.firstName} {s.lastName}
            </option>
          ))}
        </select>
      </div>

      {/* Calendar */}
      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-amber-500 border-t-transparent" />
        </div>
      ) : (
        <WeekCalendar
          appointments={appointments}
          weekDays={weekDays}
          onSlotClick={handleSlotClick}
          onAppointmentClick={setSelectedAppointment}
        />
      )}

      {/* Create Modal */}
      <Modal isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)} title="Nouveau rendez-vous">
        <form onSubmit={handleCreate} className="space-y-4">
          {/* Barber */}
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-700">Coiffeur *</label>
            <select
              name="barberId"
              required
              className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
            >
              <option value="">Choisir un coiffeur</option>
              {staff.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.firstName} {s.lastName}
                </option>
              ))}
            </select>
          </div>

          {/* Client (optional, searchable) */}
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-700">Client</label>
            <input
              type="text"
              placeholder="Rechercher un client..."
              value={clientSearch}
              onChange={(e) => setClientSearch(e.target.value)}
              className="mb-1 w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
            />
            <select
              name="clientId"
              className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
            >
              <option value="">Sans client</option>
              {clients.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.firstName} {c.lastName}
                </option>
              ))}
            </select>
          </div>

          {/* Date + Time */}
          <div className="grid grid-cols-2 gap-3">
            <div>
              <label className="mb-1 block text-sm font-medium text-stone-700">Date *</label>
              <input
                name="date"
                type="date"
                required
                defaultValue={createDefaults.date}
                className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
              />
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-stone-700">Heure *</label>
              <input
                name="time"
                type="time"
                required
                defaultValue={createDefaults.time}
                step="1800"
                className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
              />
            </div>
          </div>

          {/* Services multi-select */}
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-700">Prestations *</label>
            <div className="flex flex-wrap gap-2 rounded-md border border-stone-300 p-2 max-h-40 overflow-y-auto">
              {services.map((s) => {
                const selected = selectedServiceIds.includes(s.id);
                return (
                  <button
                    key={s.id}
                    type="button"
                    onClick={() => toggleService(s.id)}
                    className={`rounded-full px-3 py-1 text-xs font-medium transition-colors ${
                      selected
                        ? 'bg-amber-600 text-white'
                        : 'bg-stone-100 text-stone-700 hover:bg-stone-200'
                    }`}
                  >
                    {s.name} ({formatDuration(s.durationMinutes)} - {formatPrice(s.price)})
                  </button>
                );
              })}
            </div>
            {selectedServiceIds.length > 0 && (
              <div className="mt-1 text-sm text-stone-600">
                Duree totale : {formatDuration(selectedServicesDuration)} | Total : {formatPrice(selectedServicesTotal)}
              </div>
            )}
          </div>

          {/* Notes */}
          <div>
            <label className="mb-1 block text-sm font-medium text-stone-700">Notes</label>
            <textarea
              name="notes"
              rows={2}
              placeholder="Notes optionnelles..."
              className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
            />
          </div>

          {/* Buttons */}
          <div className="flex justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsCreateOpen(false)}
              className="rounded-md border border-stone-300 px-4 py-2 text-sm text-stone-700 hover:bg-stone-50"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending || selectedServiceIds.length === 0}
              className="rounded-md bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700 disabled:opacity-50"
            >
              {createMutation.isPending ? 'Creation...' : 'Creer le RDV'}
            </button>
          </div>
        </form>
      </Modal>

      {/* Detail / Status Modal */}
      <Modal
        isOpen={selectedAppointment !== null}
        onClose={() => setSelectedAppointment(null)}
        title="Detail du rendez-vous"
      >
        {selectedAppointment && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="font-medium text-stone-500">Coiffeur</span>
                <p>{selectedAppointment.barberName}</p>
              </div>
              <div>
                <span className="font-medium text-stone-500">Client</span>
                <p>{selectedAppointment.clientName ?? 'Sans client'}</p>
              </div>
              <div>
                <span className="font-medium text-stone-500">Debut</span>
                <p>{formatTime(selectedAppointment.startTime)}</p>
              </div>
              <div>
                <span className="font-medium text-stone-500">Fin</span>
                <p>{formatTime(selectedAppointment.endTime)}</p>
              </div>
              <div>
                <span className="font-medium text-stone-500">Statut</span>
                <p>{STATUS_LABELS[selectedAppointment.status]}</p>
              </div>
              <div>
                <span className="font-medium text-stone-500">Total</span>
                <p>{formatPrice(selectedAppointment.totalPrice)}</p>
              </div>
            </div>

            {/* Services list */}
            <div>
              <span className="text-sm font-medium text-stone-500">Prestations</span>
              <ul className="mt-1 space-y-1">
                {selectedAppointment.services.map((s) => (
                  <li key={s.serviceId} className="flex justify-between text-sm">
                    <span>{s.serviceName} ({formatDuration(s.durationMinutes)})</span>
                    <span>{formatPrice(s.priceApplied)}</span>
                  </li>
                ))}
              </ul>
            </div>

            {selectedAppointment.notes && (
              <div>
                <span className="text-sm font-medium text-stone-500">Notes</span>
                <p className="text-sm">{selectedAppointment.notes}</p>
              </div>
            )}

            {/* Actions */}
            <div className="flex flex-wrap gap-2 border-t border-stone-200 pt-3">
              {selectedAppointment.status === 'SCHEDULED' && (
                <button
                  onClick={() => setIsRescheduleOpen(true)}
                  className="rounded-md border border-amber-600 px-3 py-1.5 text-sm font-medium text-amber-600 hover:bg-amber-50"
                >
                  Deplacer
                </button>
              )}
              {TRANSITIONS[selectedAppointment.status].map((status) => (
                <button
                  key={status}
                  onClick={() => handleStatusChange(status)}
                  disabled={statusMutation.isPending}
                  className="rounded-md bg-amber-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-amber-700 disabled:opacity-50"
                >
                  {statusMutation.isPending ? '...' : STATUS_LABELS[status]}
                </button>
              ))}
            </div>
          </div>
        )}
      </Modal>
      {/* Reschedule Modal */}
      <Modal
        isOpen={isRescheduleOpen}
        onClose={() => setIsRescheduleOpen(false)}
        title="Deplacer le rendez-vous"
      >
        {selectedAppointment && (
          <form onSubmit={handleReschedule} className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="mb-1 block text-sm font-medium text-stone-700">Nouvelle date *</label>
                <input
                  name="rescheduleDate"
                  type="date"
                  required
                  defaultValue={selectedAppointment.startTime.split('T')[0]}
                  className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-stone-700">Nouvelle heure *</label>
                <input
                  name="rescheduleTime"
                  type="time"
                  required
                  defaultValue={selectedAppointment.startTime.split('T')[1]?.slice(0, 5)}
                  step="1800"
                  className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
                />
              </div>
            </div>
            <div>
              <label className="mb-1 block text-sm font-medium text-stone-700">Coiffeur</label>
              <select
                name="rescheduleBarberId"
                defaultValue={selectedAppointment.barberId}
                className="w-full rounded-md border border-stone-300 px-3 py-2 text-sm focus:border-amber-500 focus:outline-none focus:ring-1 focus:ring-amber-500"
              >
                {staff.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.firstName} {s.lastName}
                  </option>
                ))}
              </select>
            </div>
            <div className="flex justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setIsRescheduleOpen(false)}
                className="rounded-md border border-stone-300 px-4 py-2 text-sm text-stone-700 hover:bg-stone-50"
              >
                Annuler
              </button>
              <button
                type="submit"
                disabled={rescheduleMutation.isPending}
                className="rounded-md bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700 disabled:opacity-50"
              >
                {rescheduleMutation.isPending ? 'Deplacement...' : 'Deplacer'}
              </button>
            </div>
          </form>
        )}
      </Modal>
    </div>
  );
}
