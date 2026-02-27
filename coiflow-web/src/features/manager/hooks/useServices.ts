import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { ServiceCatalogService } from '@/shared/services/ServiceCatalogService';
import { extractErrorMessage } from '@/shared/utils/errorMessage';
import type { CreateServicePayload, UpdateServicePayload } from '@/shared/types/service';

const SERVICES_KEY = ['services'];

export function useServices() {
  return useQuery({
    queryKey: SERVICES_KEY,
    queryFn: ServiceCatalogService.getAll,
  });
}

export function useCreateService() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateServicePayload) => ServiceCatalogService.create(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SERVICES_KEY });
      toast.success('Prestation creee avec succes');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}

export function useUpdateService() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }: { id: string; payload: UpdateServicePayload }) =>
      ServiceCatalogService.update(id, payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SERVICES_KEY });
      toast.success('Prestation mise a jour');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}

export function useDeleteService() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => ServiceCatalogService.remove(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: SERVICES_KEY });
      toast.success('Prestation desactivee');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}
