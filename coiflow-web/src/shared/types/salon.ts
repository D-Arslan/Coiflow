export interface Salon {
  id: string;
  name: string;
  address: string | null;
  phone: string | null;
  email: string | null;
  active: boolean;
  managerId: string | null;
  managerName: string | null;
  createdAt: string;
}

export interface CreateSalonPayload {
  name: string;
  address?: string;
  phone?: string;
  email?: string;
  managerFirstName: string;
  managerLastName: string;
  managerEmail: string;
  managerPassword: string;
}

export interface UpdateSalonPayload {
  name: string;
  address?: string;
  phone?: string;
  email?: string;
}
