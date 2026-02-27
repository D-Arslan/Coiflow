export type Role = 'ADMIN' | 'MANAGER' | 'BARBER';

export interface UserInfo {
  userId: string;
  email: string;
  firstName: string;
  lastName: string;
  role: Role;
  salonId: string | null;
}

export interface LoginRequest {
  email: string;
  password: string;
}
