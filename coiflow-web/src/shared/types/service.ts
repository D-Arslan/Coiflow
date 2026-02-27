export interface ServiceItem {
  id: string;
  name: string;
  durationMinutes: number;
  price: number;
  active: boolean;
  createdAt: string;
}

export interface CreateServicePayload {
  name: string;
  durationMinutes: number;
  price: number;
}

export interface UpdateServicePayload {
  name: string;
  durationMinutes: number;
  price: number;
}
