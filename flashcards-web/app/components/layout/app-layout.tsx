import type { ReactNode } from "react";
import { Link, Navigate, NavLink, Outlet, useNavigate } from "react-router";
import { useAuth } from "~/hooks/use-auth";
import { cn, getInitials } from "~/lib/utils";
import { ThemeToggle } from "~/components/ui/theme-toggle";
import { Spinner } from "~/components/ui/spinner";

interface NavItem {
  to: string;
  label: string;
  icon: ReactNode;
}

function IconGenerate() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
    </svg>
  );
}

function IconDecks() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M6.429 9.75 2.25 12l4.179 2.25m0-4.5 5.571 3 5.571-3m-11.142 0L2.25 7.5 12 2.25l9.75 5.25-4.179 2.25m0 0L21.75 12l-4.179 2.25m0 0 4.179 2.25L12 21.75 2.25 16.5l4.179-2.25m11.142 0-5.571 3-5.571-3" />
    </svg>
  );
}

function IconStudy() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M12 6.042A8.967 8.967 0 0 0 6 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 0 1 6 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 0 1 6-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0 0 18 18a8.967 8.967 0 0 0-6 2.292m0-14.25v14.25" />
    </svg>
  );
}

function IconFiles() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M19.5 14.25v-2.625a3.375 3.375 0 0 0-3.375-3.375h-1.5A1.125 1.125 0 0 1 13.5 7.125v-1.5a3.375 3.375 0 0 0-3.375-3.375H8.25m2.25 0H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 0 0-9-9Z" />
    </svg>
  );
}

function IconSettings() {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" strokeWidth={1.5} stroke="currentColor" className="h-6 w-6" aria-hidden="true">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9.594 3.94c.09-.542.56-.94 1.11-.94h2.593c.55 0 1.02.398 1.11.94l.213 1.281c.063.374.313.686.645.87.074.04.147.083.22.127.325.196.72.257 1.075.124l1.217-.456a1.125 1.125 0 0 1 1.37.49l1.296 2.247a1.125 1.125 0 0 1-.26 1.431l-1.003.827c-.293.24-.438.613-.431.992a6.759 6.759 0 0 1 0 .255c-.007.378.138.75.43.99l1.005.828c.424.35.534.954.26 1.43l-1.298 2.247a1.125 1.125 0 0 1-1.369.491l-1.217-.456c-.355-.133-.75-.072-1.076.124a6.57 6.57 0 0 1-.22.128c-.331.183-.581.495-.644.869l-.213 1.28c-.09.543-.56.941-1.11.941h-2.594c-.55 0-1.02-.398-1.11-.94l-.213-1.281c-.062-.374-.312-.686-.644-.87a6.52 6.52 0 0 1-.22-.127c-.325-.196-.72-.257-1.076-.124l-1.217.456a1.125 1.125 0 0 1-1.369-.49l-1.297-2.247a1.125 1.125 0 0 1 .26-1.431l1.004-.827c.292-.24.437-.613.43-.992a6.932 6.932 0 0 1 0-.255c.007-.378-.138-.75-.43-.99l-1.004-.828a1.125 1.125 0 0 1-.26-1.43l1.297-2.247a1.125 1.125 0 0 1 1.37-.491l1.216.456c.356.133.751.072 1.076-.124.072-.044.146-.087.22-.128.332-.183.582-.495.644-.869l.214-1.281Z" />
      <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z" />
    </svg>
  );
}

const NAV_ITEMS: NavItem[] = [
  { to: "/generate", label: "Gerar", icon: <IconGenerate /> },
  { to: "/decks", label: "Decks", icon: <IconDecks /> },
  { to: "/study", label: "Estudar", icon: <IconStudy /> },
  { to: "/files", label: "Arquivos", icon: <IconFiles /> },
  { to: "/settings", label: "Ajustes", icon: <IconSettings /> },
];

export function AppLayout() {
  const { user, isAuthenticated, isReady, logout } = useAuth();
  const navigate = useNavigate();

  if (!isReady) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-base-100">
        <Spinner size="lg" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  return (
    <div className="flex min-h-screen flex-col bg-base-100">
      <header className="navbar sticky top-0 z-30 border-b border-base-200 bg-base-100/80 px-2 backdrop-blur-md">
        <div className="navbar-start">
          <Link to="/generate" className="flex items-center gap-2 px-2">
            <img src="/logo.svg" alt="" className="h-8 w-8 rounded-box" />
            <span className="font-display text-lg font-bold tracking-tight">
              Flashcards
            </span>
          </Link>
        </div>

        <div className="navbar-end gap-1">
          <ThemeToggle />

          <div className="dropdown dropdown-end">
            <div
              tabIndex={0}
              role="button"
              className="btn btn-ghost gap-2 px-2"
              aria-label={`Menu do usuário: ${user?.name ?? ""}`}
            >
              <div className="avatar avatar-placeholder">
                <div className="w-8 rounded-full bg-primary text-primary-content">
                  <span className="text-xs font-semibold">
                    {getInitials(user?.name ?? user?.email)}
                  </span>
                </div>
              </div>
            </div>
            <ul
              tabIndex={0}
              className="menu dropdown-content z-50 w-56 rounded-box border border-base-200 bg-base-100 p-2 shadow-lg"
            >
              <li>
                <div className="flex flex-col items-start gap-0.5">
                  <span className="truncate text-sm font-medium">
                    {user?.name}
                  </span>
                  <span className="truncate text-xs text-base-content/60">
                    {user?.email}
                  </span>
                </div>
              </li>
              <li className="mt-1 border-t border-base-200">
                <button type="button" className="text-error" onClick={handleLogout}>
                  Sair
                </button>
              </li>
            </ul>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-3xl flex-1 px-4 pb-safe-nav pt-4 sm:pb-8">
        <Outlet />
      </main>

      {/* Navegação inferior (mobile-first) — daisyUI 5 "dock" */}
      <nav
        className="dock dock-sm z-30 border-t border-base-200 bg-base-100/95 backdrop-blur-md sm:dock-md"
        aria-label="Navegação principal"
      >
        {NAV_ITEMS.map((item) => (
          <NavLink
            key={item.to}
            to={item.to}
            className={({ isActive }) =>
              cn(isActive && "dock-active text-primary")
            }
          >
            {item.icon}
            <span className="dock-label text-[11px]">{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  );
}

export default AppLayout;
