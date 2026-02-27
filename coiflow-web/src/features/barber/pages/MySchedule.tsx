import { useState, useMemo } from 'react';
import { useAuth } from '@/shared/context/AuthContext';
import { useAppointments } from '@/features/manager/hooks/useAppointments';
import { WeekCalendar } from '@/shared/components/WeekCalendar';
import { Modal } from '@/shared/components/Modal';
import { getWeekRange, addDays, formatDateISO, formatTime } from '@/shared/utils/dateHelpers';
import { formatPrice, formatDuration } from '@/shared/utils/formatters';
import type { Appointment, AppointmentStatus } from '@/shared/types/appointment';

const STATUS_LABELS: Record<AppointmentStatus, string> = {
  SCHEDULED: 'Planifie',
  IN_PROGRESS: 'En cours',
  COMPLETED: 'Termine',
  CANCELLED: 'Annule',
  NO_SHOW: 'Absent',
};

export default function MySchedule() {
  const { user } = useAuth();
  const [currentWeek, setCurrentWeek] = useState(() => new Date());
  const [selectedAppointment, setSelectedAppointment] = useState<Appointment | null>(null);

  const { start, end } = useMemo(() => getWeekRange(currentWeek), [currentWeek]);
  const weekDays = useMemo(() => {
    const days: Date[] = [];
    for (let i = 0; i < 7; i++) days.push(addDays(start, i));
    return days;
  }, [start]);

  const startStr = formatDateISO(start);
  const endStr = formatDateISO(end);

  const { data: appointments = [], isLoading } = useAppointments(startStr, endStr, user?.userId);

  return (
    <div className="space-y-6">
      <h2 className="text-xl font-semibold text-gray-900">Mon planning</h2>

      {/* Week navigation */}
      <div className="flex items-center gap-2">
        <button
          onClick={() => setCurrentWeek((w) => addDays(w, -7))}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
        >
          &larr; Semaine prec.
        </button>
        <button
          onClick={() => setCurrentWeek(new Date())}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
        >
          Aujourd'hui
        </button>
        <button
          onClick={() => setCurrentWeek((w) => addDays(w, 7))}
          className="rounded-md border border-gray-300 px-3 py-1.5 text-sm text-gray-700 hover:bg-gray-50"
        >
          Semaine suiv. &rarr;
        </button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-4 border-blue-600 border-t-transparent" />
        </div>
      ) : (
        <WeekCalendar
          appointments={appointments}
          weekDays={weekDays}
          onAppointmentClick={setSelectedAppointment}
        />
      )}

      {/* Detail Modal (read-only) */}
      <Modal
        isOpen={selectedAppointment !== null}
        onClose={() => setSelectedAppointment(null)}
        title="Detail du rendez-vous"
      >
        {selectedAppointment && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3 text-sm">
              <div>
                <span className="font-medium text-gray-500">Client</span>
                <p>{selectedAppointment.clientName ?? 'Sans client'}</p>
              </div>
              <div>
                <span className="font-medium text-gray-500">Statut</span>
                <p>{STATUS_LABELS[selectedAppointment.status]}</p>
              </div>
              <div>
                <span className="font-medium text-gray-500">Debut</span>
                <p>{formatTime(selectedAppointment.startTime)}</p>
              </div>
              <div>
                <span className="font-medium text-gray-500">Fin</span>
                <p>{formatTime(selectedAppointment.endTime)}</p>
              </div>
              <div>
                <span className="font-medium text-gray-500">Total</span>
                <p>{formatPrice(selectedAppointment.totalPrice)}</p>
              </div>
            </div>

            <div>
              <span className="text-sm font-medium text-gray-500">Prestations</span>
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
                <span className="text-sm font-medium text-gray-500">Notes</span>
                <p className="text-sm">{selectedAppointment.notes}</p>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
