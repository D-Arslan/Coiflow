import { useMemo } from 'react';
import type { Appointment, AppointmentStatus } from '@/shared/types/appointment';
import { formatTime, isSameDay, getDayName, formatDateISO } from '@/shared/utils/dateHelpers';

const HOUR_START = 8;
const HOUR_END = 20;
const SLOT_HEIGHT = 48; // px per 30min slot
const TOTAL_SLOTS = (HOUR_END - HOUR_START) * 2;

const STATUS_COLORS: Record<AppointmentStatus, string> = {
  SCHEDULED: 'bg-green-200 border-green-400 text-green-900',
  IN_PROGRESS: 'bg-blue-200 border-blue-400 text-blue-900',
  COMPLETED: 'bg-gray-200 border-gray-400 text-gray-700',
  CANCELLED: 'bg-red-200 border-red-400 text-red-900',
  NO_SHOW: 'bg-orange-200 border-orange-400 text-orange-900',
};

interface WeekCalendarProps {
  appointments: Appointment[];
  weekDays: Date[];
  onSlotClick?: (day: Date, hour: number, minute: number) => void;
  onAppointmentClick?: (appointment: Appointment) => void;
}

function getSlotPosition(dateStr: string): { top: number; height: number } | null {
  const date = new Date(dateStr);
  const hours = date.getHours();
  const minutes = date.getMinutes();
  if (hours < HOUR_START || hours >= HOUR_END) return null;
  const slotsFromTop = (hours - HOUR_START) * 2 + minutes / 30;
  return { top: slotsFromTop * SLOT_HEIGHT, height: 0 };
}

function getBlockStyle(appointment: Appointment): { top: number; height: number } | null {
  const startPos = getSlotPosition(appointment.startTime);
  if (!startPos) return null;
  const startDate = new Date(appointment.startTime);
  const endDate = new Date(appointment.endTime);
  const durationMinutes = (endDate.getTime() - startDate.getTime()) / 60_000;
  const height = Math.max((durationMinutes / 30) * SLOT_HEIGHT, SLOT_HEIGHT / 2);
  return { top: startPos.top, height };
}

export function WeekCalendar({ appointments, weekDays, onSlotClick, onAppointmentClick }: WeekCalendarProps) {
  const hours = useMemo(() => {
    const h: string[] = [];
    for (let i = HOUR_START; i < HOUR_END; i++) {
      h.push(`${String(i).padStart(2, '0')}:00`);
    }
    return h;
  }, []);

  const appointmentsByDay = useMemo(() => {
    const map = new Map<string, Appointment[]>();
    for (const day of weekDays) {
      const key = formatDateISO(day);
      map.set(key, []);
    }
    for (const apt of appointments) {
      const aptDate = new Date(apt.startTime);
      for (const day of weekDays) {
        if (isSameDay(aptDate, day)) {
          const key = formatDateISO(day);
          map.get(key)!.push(apt);
          break;
        }
      }
    }
    return map;
  }, [appointments, weekDays]);

  const totalHeight = TOTAL_SLOTS * SLOT_HEIGHT;

  return (
    <div className="overflow-x-auto rounded-lg border border-gray-200 bg-white">
      <div className="min-w-[800px]">
        {/* Header row */}
        <div className="grid border-b border-gray-200" style={{ gridTemplateColumns: '60px repeat(7, 1fr)' }}>
          <div className="border-r border-gray-200 p-2" />
          {weekDays.map((day) => {
            const isToday = isSameDay(day, new Date());
            return (
              <div
                key={formatDateISO(day)}
                className={`border-r border-gray-200 p-2 text-center text-sm font-medium last:border-r-0 ${isToday ? 'bg-blue-50 text-blue-700' : 'text-gray-700'}`}
              >
                <div>{getDayName(day)}</div>
                <div className={`text-lg ${isToday ? 'font-bold' : ''}`}>{day.getDate()}</div>
              </div>
            );
          })}
        </div>

        {/* Body */}
        <div className="grid" style={{ gridTemplateColumns: '60px repeat(7, 1fr)' }}>
          {/* Time column */}
          <div className="relative border-r border-gray-200" style={{ height: totalHeight }}>
            {hours.map((label, i) => (
              <div
                key={label}
                className="absolute left-0 right-0 border-b border-gray-100 px-1 text-xs text-gray-400"
                style={{ top: i * SLOT_HEIGHT * 2, height: SLOT_HEIGHT * 2 }}
              >
                {label}
              </div>
            ))}
          </div>

          {/* Day columns */}
          {weekDays.map((day) => {
            const key = formatDateISO(day);
            const dayAppointments = appointmentsByDay.get(key) ?? [];
            return (
              <div
                key={key}
                className="relative border-r border-gray-200 last:border-r-0"
                style={{ height: totalHeight }}
              >
                {/* Grid lines */}
                {hours.map((label, i) => (
                  <div
                    key={label}
                    className="absolute left-0 right-0 border-b border-gray-100 cursor-pointer hover:bg-gray-50"
                    style={{ top: i * SLOT_HEIGHT * 2, height: SLOT_HEIGHT }}
                    onClick={() => onSlotClick?.(day, HOUR_START + i, 0)}
                  />
                ))}
                {hours.map((label, i) => (
                  <div
                    key={`${label}-30`}
                    className="absolute left-0 right-0 border-b border-gray-50 cursor-pointer hover:bg-gray-50"
                    style={{ top: i * SLOT_HEIGHT * 2 + SLOT_HEIGHT, height: SLOT_HEIGHT }}
                    onClick={() => onSlotClick?.(day, HOUR_START + i, 30)}
                  />
                ))}

                {/* Appointments */}
                {dayAppointments.map((apt) => {
                  const style = getBlockStyle(apt);
                  if (!style) return null;
                  return (
                    <div
                      key={apt.id}
                      className={`absolute left-0.5 right-0.5 cursor-pointer overflow-hidden rounded border px-1 py-0.5 text-xs ${STATUS_COLORS[apt.status]}`}
                      style={{ top: style.top, height: style.height }}
                      onClick={(e) => {
                        e.stopPropagation();
                        onAppointmentClick?.(apt);
                      }}
                    >
                      <div className="font-medium truncate">
                        {formatTime(apt.startTime)} - {apt.clientName ?? 'Sans client'}
                      </div>
                      {style.height > SLOT_HEIGHT && (
                        <div className="truncate text-[10px] opacity-80">{apt.barberName}</div>
                      )}
                    </div>
                  );
                })}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
