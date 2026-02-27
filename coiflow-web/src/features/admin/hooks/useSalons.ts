import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { SalonService } from '@/shared/services/SalonService';
import { extractErrorMessage } from '@/shared/utils/errorMessage';
import type { CreateSalonPayload, UpdateSalonPayload } from '@/shared/types/salon';

const SALONS_KEY = ['salons'];

export function useSalons() {
  return useQuery({
    queryKey: SALONS_KEY,
    queryFn: SalonService.getAll,
  });
}

export function useCreateSalon() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateSalonPayload) => SalonService.create(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SALONS_KEY });
      toast.success('Salon cree avec succes');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}

export function useUpdateSalon() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateSalonPayload }) =>
      SalonService.update(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SALONS_KEY });
      toast.success('Salon mis a jour');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}

export function useToggleSalon() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => SalonService.toggle(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SALONS_KEY });
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}
