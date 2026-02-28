import { Navigate } from 'react-router-dom';
import { useAuth } from '@/shared/context/AuthContext';
import type { Role } from '@/shared/types/auth';

interface ProtectedRouteProps {
  children: React.ReactNode;
  allowedRoles: Role[];
}

const DEFAULT_ROUTES: Record<Role, string> = {
  ADMIN: '/admin',
  MANAGER: '/manager',
  BARBER: '/barber',
};

export function ProtectedRoute({ children, allowedRoles }: ProtectedRouteProps) {
  const { isAuthenticated, role, authReady } = useAuth();

  if (!authReady) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-amber-500" />
      </div>
    );
  }

  if (!isAuthenticated || !role) {
    return <Navigate to="/login" replace />;
  }

  if (!allowedRoles.includes(role)) {
    return <Navigate to={DEFAULT_ROUTES[role]} replace />;
  }

  return <>{children}</>;
}
