import { type ReactNode, type ElementType } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import { LogOut } from 'lucide-react';
import { useAuth } from '@/shared/context/AuthContext';

export interface NavItem {
  label: string;
  to: string;
  icon?: ElementType;
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
    <div className="flex h-screen bg-stone-100">
      <aside className="w-64 bg-slate-900 shadow-xl flex flex-col">
        <div className="h-16 flex items-center px-6 border-b border-slate-700/50">
          <span className="text-xl font-bold text-amber-400">Coiflow</span>
        </div>
        <nav className="flex-1 px-3 py-6 space-y-1">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === navItems[0]?.to}
              className={({ isActive }) =>
                `flex items-center gap-3 px-4 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-amber-500/15 text-amber-400'
                    : 'text-slate-400 hover:bg-slate-800 hover:text-slate-200'
                }`
              }
            >
              {item.icon && <item.icon className="h-5 w-5 shrink-0" />}
              {item.label}
            </NavLink>
          ))}
        </nav>
        <div className="p-4 border-t border-slate-700/50">
          <p className="text-sm text-slate-400 truncate">
            {user?.firstName} {user?.lastName}
          </p>
          <button
            onClick={handleLogout}
            className="mt-2 w-full flex items-center gap-2 text-left text-sm text-slate-500 hover:text-red-400 transition-colors"
          >
            <LogOut className="h-4 w-4" />
            Deconnexion
          </button>
        </div>
      </aside>

      <div className="flex-1 flex flex-col overflow-hidden">
        <header className="h-16 bg-white shadow-sm border-b border-stone-200 flex items-center px-6">
          <h1 className="text-lg font-semibold text-stone-800">{title}</h1>
        </header>
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
