import { type ReactNode } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '@/shared/context/AuthContext';

export interface NavItem {
  label: string;
  to: string;
}

interface MainLayoutProps {
  title: string;
  navItems: NavItem[];
  children: ReactNode;
}

export default function MainLayout({ title, navItems, children }: MainLayoutProps) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex h-screen bg-gray-100">
      <aside className="w-64 bg-white shadow-md flex flex-col">
        <div className="h-16 flex items-center px-6 border-b">
          <span className="text-xl font-bold text-blue-600">Coiflow</span>
        </div>
        <nav className="flex-1 px-4 py-6 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === navItems[0]?.to}
              className={({ isActive }) =>
                `block px-4 py-2 rounded-md text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-50 text-blue-700'
                    : 'text-gray-700 hover:bg-gray-50'
                }`
              }
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="p-4 border-t">
          <p className="text-sm text-gray-600 truncate">
            {user?.firstName} {user?.lastName}
          </p>
          <button
            onClick={handleLogout}
            className="mt-2 w-full text-left text-sm text-red-600 hover:text-red-800"
          >
            Deconnexion
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col overflow-hidden">
        <header className="h-16 bg-white shadow-sm flex items-center px-6">
          <h1 className="text-lg font-semibold text-gray-900">{title}</h1>
        </header>
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
