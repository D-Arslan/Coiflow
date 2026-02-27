import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { AppointmentService } from '@/shared/services/AppointmentService';
import { extractErrorMessage } from '@/shared/utils/errorMessage';
import type { CreateAppointmentPayload, AppointmentStatus } from '@/shared/types/appointment';

const APPOINTMENTS_KEY = ['appointments'];

export function useAppointments(start: string, end: string, barberId?: string) {
  return useQuery({
    queryKey: barberId ? [...APPOINTMENTS_KEY, start, end, barberId] : [...APPOINTMENTS_KEY, start, end],
    queryFn: () => AppointmentService.getAll(start, end, barberId),
  });
}

export function useAppointmentsToCash(start: string, end: string) {
  return useQuery({
    queryKey: [...APPOINTMENTS_KEY, 'to-cash', start, end],
    queryFn: () => AppointmentService.getToCash(start, end),
  });
}

export function useCreateAppointment() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateAppointmentPayload) => AppointmentService.create(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: APPOINTMENTS_KEY });
      toast.success('Rendez-vous cree avec succes');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}

export function useUpdateStatus() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: AppointmentStatus }) =>
      AppointmentService.updateStatus(id, status),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: APPOINTMENTS_KEY });
      toast.success('Statut mis a jour');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}
