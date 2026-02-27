import { createContext, useContext, useEffect, useState, useCallback, type ReactNode } from 'react';
import { AuthService } from '@/shared/services/AuthService';
import type { UserInfo, Role } from '@/shared/types/auth';

interface AuthContextType {
  user: UserInfo | null;
  role: Role | null;
  isAuthenticated: boolean;
  authReady: boolean;
  login: (user: UserInfo) => void;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserInfo | null>(null);
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    AuthService.me()
      .then((userData) => setUser(userData))
      .catch(() => setUser(null))
      .finally(() => setAuthReady(true));
  }, []);

  const login = useCallback((userData: UserInfo) => {
    setUser(userData);
  }, []);

  const logout = useCallback(async () => {
    try {
      await AuthService.logout();
    } finally {
      setUser(null);
    }
  }, []);

  return (
    <AuthContext.Provider
      value={{
        user,
        role: user?.role ?? null,
        isAuthenticated: user !== null,
        authReady,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
