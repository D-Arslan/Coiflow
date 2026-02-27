export interface Client {
  id: string;
  firstName: string;
  lastName: string;
  phone: string | null;
  email: string | null;
  notes: string | null;
  createdAt: string;
}

export interface CreateClientPayload {
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  notes?: string;
}

export interface UpdateClientPayload {
  firstName: string;
  lastName: string;
  phone?: string;
  email?: string;
  notes?: string;
}
