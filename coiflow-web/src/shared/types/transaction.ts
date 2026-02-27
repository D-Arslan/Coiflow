export type PaymentMethod = 'CASH' | 'CARD' | 'CHECK' | 'TRANSFER' | 'OTHER';
export type TransactionStatus = 'COMPLETED' | 'VOIDED';

export interface PaymentLine {
  method: string;
  amount: number;
}

export interface Transaction {
  id: string;
  appointmentId: string | null;
  barberId: string;
  barberName: string;
  totalAmount: number;
  status: TransactionStatus;
  payments: PaymentLine[];
  commissionRate: number | null;
  commissionAmount: number | null;
  createdBy: string;
  createdAt: string;
}

export interface PaymentLinePayload {
  method: PaymentMethod;
  amount: number;
}

export interface CreateTransactionPayload {
  appointmentId: string;
  payments: PaymentLinePayload[];
}
