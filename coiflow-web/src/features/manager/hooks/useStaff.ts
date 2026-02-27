import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { StaffService } from '@/shared/services/StaffService';
import { extractErrorMessage } from '@/shared/utils/errorMessage';
import type { CreateStaffPayload, UpdateStaffPayload } from '@/shared/types/staff';

const STAFF_KEY = ['staff'];

export function useStaff() {
  return useQuery({
    queryKey: STAFF_KEY,
    queryFn: StaffService.getAll,
  });
}

export function useCreateStaff() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateStaffPayload) => StaffService.create(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: STAFF_KEY });
      toast.success('Coiffeur ajoute avec succes');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}

export function useUpdateStaff() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateStaffPayload }) =>
      StaffService.update(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: STAFF_KEY });
      toast.success('Coiffeur mis a jour');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}

export function useDeleteStaff() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => StaffService.remove(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: STAFF_KEY });
      toast.success('Coiffeur desactive');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}
