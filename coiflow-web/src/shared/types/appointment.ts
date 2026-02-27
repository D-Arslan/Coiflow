export type AppointmentStatus = 'SCHEDULED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';

export interface ServiceLine {
  serviceId: string;
  serviceName: string;
  priceApplied: number;
  durationMinutes: number;
}

export interface Appointment {
  id: string;
  barberId: string;
  barberName: string;
  clientId: string | null;
  clientName: string | null;
  startTime: string;
  endTime: string;
  status: AppointmentStatus;
  notes: string | null;
  services: ServiceLine[];
  totalPrice: number;
  createdAt: string;
}

export interface CreateAppointmentPayload {
  barberId: string;
  clientId?: string;
  startTime: string;
  serviceIds: string[];
  notes?: string;
}
