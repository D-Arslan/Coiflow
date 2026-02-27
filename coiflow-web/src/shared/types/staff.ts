export interface Staff {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  commissionRate: number;
  active: boolean;
  createdAt: string;
}

export interface CreateStaffPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  commissionRate: number;
}

export interface UpdateStaffPayload {
  firstName: string;
  lastName: string;
  commissionRate: number;
}
