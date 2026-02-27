export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/api/auth/login',
    LOGOUT: '/api/auth/logout',
    REFRESH: '/api/auth/refresh',
    ME: '/api/auth/me',
  },
  ADMIN: {
    SALONS: '/api/admin/salons',
  },
  STAFF: '/api/staff',
  SERVICES: '/api/services',
  CLIENTS: '/api/clients',
  APPOINTMENTS: '/api/appointments',
  TRANSACTIONS: '/api/transactions',
  COMMISSIONS: '/api/commissions',
  DASHBOARD: {
    STATS: '/api/dashboard/stats',
    REVENUE: '/api/dashboard/revenue',
  },
} as const;
