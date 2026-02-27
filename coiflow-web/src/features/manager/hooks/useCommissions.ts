import { useQuery } from '@tanstack/react-query';
import { CommissionService } from '@/shared/services/CommissionService';

const COMMISSIONS_KEY = ['commissions'];

export function useCommissions(start: string, end: string, barberId?: string) {
  return useQuery({
    queryKey: barberId ? [...COMMISSIONS_KEY, start, end, barberId] : [...COMMISSIONS_KEY, start, end],
    queryFn: () => CommissionService.getAll(start, end, barberId),
  });
}
