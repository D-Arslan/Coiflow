import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { ClientService } from '@/shared/services/ClientService';
import { extractErrorMessage } from '@/shared/utils/errorMessage';
import type { CreateClientPayload, UpdateClientPayload } from '@/shared/types/client';

const CLIENTS_KEY = ['clients'];

export function useClients(search: string) {
  return useQuery({
    queryKey: [...CLIENTS_KEY, search],
    queryFn: () => ClientService.getAll(search || undefined),
  });
}

export function useCreateClient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateClientPayload) => ClientService.create(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: CLIENTS_KEY });
      toast.success('Client cree avec succes');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}

export function useUpdateClient() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateClientPayload }) =>
      ClientService.update(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: CLIENTS_KEY });
      toast.success('Client mis a jour');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}
