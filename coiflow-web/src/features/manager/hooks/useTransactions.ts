import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { toast } from 'react-toastify';
import { TransactionService } from '@/shared/services/TransactionService';
import { extractErrorMessage } from '@/shared/utils/errorMessage';
import type { CreateTransactionPayload } from '@/shared/types/transaction';

const TRANSACTIONS_KEY = ['transactions'];
const APPOINTMENTS_KEY = ['appointments'];

export function useTransactions(start: string, end: string) {
  return useQuery({
    queryKey: [...TRANSACTIONS_KEY, start, end],
    queryFn: () => TransactionService.getAll(start, end),
  });
}

export function useCreateTransaction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload: CreateTransactionPayload) => TransactionService.create(payload),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TRANSACTIONS_KEY });
      void queryClient.invalidateQueries({ queryKey: APPOINTMENTS_KEY });
      toast.success('Transaction enregistree avec succes');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}

export function useVoidTransaction() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => TransactionService.voidTransaction(id),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: TRANSACTIONS_KEY });
      toast.success('Transaction annulee');
    },
    onError: (err: unknown) => {
      toast.error(extractErrorMessage(err));
    },
  });
}
