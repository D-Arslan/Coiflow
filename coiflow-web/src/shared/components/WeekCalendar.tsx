import { useMemo, useRef, useEffect } from 'react';
import type { Appointment, AppointmentStatus } from '@/shared/types/appointment';
import { formatTime, isSameDay, getDayName, formatDateISO, addDays } from '@/shared/utils/dateHelpers';

const HOUR_START = 0;
const HOUR_END = 24;
const SLOT_HEIGHT = 48; // px per 30min slot
const TOTAL_SLOTS = (HOUR_END - HOUR_START) * 2;
const DEFAULT_SCROLL_HOUR = 8;

const STATUS_COLORS: Record<AppointmentStatus, string> = {
  SCHEDULED: 'bg-amber-200 border-amber-400 text-amber-900',
  IN_PROGRESS: 'bg-sky-200 border-sky-400 text-sky-900',
  COMPLETED: 'bg-emerald-200 border-emerald-400 text-emerald-800',
  CANCELLED: 'bg-red-200 border-red-400 text-red-900',
  NO_SHOW: 'bg-stone-300 border-stone-400 text-stone-700',
};

const INACTIVE_STATUSES: AppointmentStatus[] = ['CANCELLED', 'NO_SHOW'];

interface CalendarBlock {
  appointment: Appointment;
  topMinutes: number;
  heightMinutes: number;
}

interface WeekCalendarProps {
  appointments: Appointment[];
  weekDays: Date[];
  onSlotClick?: (day: Date, hour: number, minute: number) => void;
  onAppointmentClick?: (appointment: Appointment) => void;
}

function minutesToPx(minutes: number): number {
  return (minutes / 30) * SLOT_HEIGHT;
}

export function WeekCalendar({ appointments, weekDays, onSlotClick, onAppointmentClick }: WeekCalendarProps) {
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: DEFAULT_SCROLL_HOUR * 2 * SLOT_HEIGHT });
  }, []);

  const hours = useMemo(() => {
    const h: string[] = [];
    for (let i = HOUR_START; i < HOUR_END; i++) {
      h.push(`${String(i).padStart(2, '0')}:00`);
    }
    return h;
  }, []);

  const blocksByDay = useMemo(() => {
    const map = new Map<string, CalendarBlock[]>();
    for (const day of weekDays) {
      map.set(formatDateISO(day), []);
    }

    for (const apt of appointments) {
      const startDate = new Date(apt.startTime);
      const endDate = new Date(apt.endTime);

      for (const day of weekDays) {
        const dayKey = formatDateISO(day);
        const blocks = map.get(dayKey);
        if (!blocks) continue;

        if (isSameDay(startDate, day)) {
          if (isSameDay(endDate, day) || (endDate.getHours() === 0 && endDate.getMinutes() === 0 && isSameDay(addDays(day, 1), endDate))) {
            // Same-day or ends exactly at midnight
            const topMinutes = startDate.getHours() * 60 + startDate.getMinutes();
            const heightMinutes = (endDate.getTime() - startDate.getTime()) / 60_000;
            blocks.push({ appointment: apt, topMinutes, heightMinutes: Math.max(heightMinutes, 15) });
          } else {
            // Starts today, ends tomorrow: render from startTime to midnight
            const topMinutes = startDate.getHours() * 60 + startDate.getMinutes();
            const heightMinutes = 24 * 60 - topMinutes;
            if (heightMinutes > 0) {
              blocks.push({ appointment: apt, topMinutes, heightMinutes });
            }
          }
        } else {
          // Check if appointment spills into this day from the previous day
          const prevDay = addDays(day, -1);
          if (isSameDay(startDate, prevDay) && isSameDay(endDate, day)) {
            const heightMinutes = endDate.getHours() * 60 + endDate.getMinutes();
            if (heightMinutes > 0) {
              blocks.push({ appointment: apt, topMinutes: 0, heightMinutes });
            }
          }
        }
      }
    }
    return map;
  }, [appointments, weekDays]);

  const totalHeight = TOTAL_SLOTS * SLOT_HEIGHT;

  const scrollToHour = (hour: number) => {
    scrollRef.current?.scrollTo({ top: hour * 2 * SLOT_HEIGHT, behavior: 'smooth' });
  };

  return (
    <div className="overflow-x-auto rounded-lg border border-stone-200 bg-white">
      <div className="min-w-[800px]">
        {/* Header row — outside scroll */}
        <div className="grid border-b border-stone-200" style={{ gridTemplateColumns: '60px repeat(7, 1fr)' }}>
          <div className="border-r border-stone-200 p-2 flex items-center justify-center">
            <button
              type="button"
              onClick={() => scrollToHour(DEFAULT_SCROLL_HOUR)}
              className="text-[10px] text-stone-400 hover:text-amber-600 transition-colors"
              title="Revenir a 08:00"
            >
              08:00
            </button>
          </div>
          {weekDays.map((day) => {
            const isToday = isSameDay(day, new Date());
            return (
              <div
                key={formatDateISO(day)}
                className={`border-r border-stone-200 p-2 text-center text-sm font-medium last:border-r-0 ${isToday ? 'bg-amber-50 text-amber-700' : 'text-stone-700'}`}
              >
                <div>{getDayName(day)}</div>
                <div className={`text-lg ${isToday ? 'font-bold' : ''}`}>{day.getDate()}</div>
              </div>
            );
          })}
        </div>

        {/* Scrollable body */}
        <div ref={scrollRef} className="overflow-y-auto max-h-[700px]">
          <div className="grid" style={{ gridTemplateColumns: '60px repeat(7, 1fr)' }}>
            {/* Time column */}
            <div className="relative border-r border-stone-200" style={{ height: totalHeight }}>
              {hours.map((label, i) => (
                <div
                  key={label}
                  className="absolute left-0 right-0 border-b border-stone-100 px-1 text-xs text-stone-400"
                  style={{ top: i * SLOT_HEIGHT * 2, height: SLOT_HEIGHT * 2 }}
                >
                  {label}
                </div>
              ))}
            </div>

            {/* Day columns */}
            {weekDays.map((day) => {
              const dayKey = formatDateISO(day);
              const dayBlocks = blocksByDay.get(dayKey) ?? [];
              return (
                <div
                  key={dayKey}
                  className="relative border-r border-stone-200 last:border-r-0"
                  style={{ height: totalHeight }}
                >
                  {/* Grid lines — full hour */}
                  {hours.map((label, i) => (
                    <div
                      key={label}
                      className="absolute left-0 right-0 border-b border-stone-100 cursor-pointer hover:bg-amber-50/30"
                      style={{ top: i * SLOT_HEIGHT * 2, height: SLOT_HEIGHT }}
                      onClick={() => onSlotClick?.(day, HOUR_START + i, 0)}
                    />
                  ))}
                  {/* Grid lines — half hour */}
                  {hours.map((label, i) => (
                    <div
                      key={`${label}-30`}
                      className="absolute left-0 right-0 border-b border-stone-50 cursor-pointer hover:bg-amber-50/30"
                      style={{ top: i * SLOT_HEIGHT * 2 + SLOT_HEIGHT, height: SLOT_HEIGHT }}
                      onClick={() => onSlotClick?.(day, HOUR_START + i, 30)}
                    />
                  ))}

                  {/* Appointment blocks */}
                  {dayBlocks.map((block) => {
                    const { appointment: apt, topMinutes, heightMinutes } = block;
                    const top = minutesToPx(topMinutes);
                    const height = Math.max(minutesToPx(heightMinutes), SLOT_HEIGHT / 2);
                    const isInactive = INACTIVE_STATUSES.includes(apt.status);

                    return (
                      <div
                        key={`${apt.id}-${topMinutes}`}
                        className={`absolute left-0.5 right-0.5 overflow-hidden rounded border px-1 py-0.5 text-xs ${STATUS_COLORS[apt.status]} ${isInactive ? 'opacity-40' : 'cursor-pointer'}`}
                        style={{ top, height }}
                        onClick={(e) => {
                          if (isInactive) return;
                          e.stopPropagation();
                          onAppointmentClick?.(apt);
                        }}
                      >
                        <div className={`font-medium truncate ${isInactive ? 'line-through' : ''}`}>
                          {formatTime(apt.startTime)} - {apt.clientName ?? 'Sans client'}
                        </div>
                        {height > SLOT_HEIGHT && (
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
    </div>
  );
}
