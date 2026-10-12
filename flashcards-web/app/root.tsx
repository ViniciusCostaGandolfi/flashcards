import { useEffect } from "react";
import {
  isRouteErrorResponse,
  Links,
  Meta,
  Outlet,
  Scripts,
  ScrollRestoration,
} from "react-router";
import {
  createContext,
  useCallback,
  useContext,
  useEffect as useThemeEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { QueryClientProvider } from "@tanstack/react-query";

import type { Route } from "./+types/root";
import { queryClient } from "~/lib/query";
import { AuthProvider } from "~/hooks/use-auth";
import "./app.css";

type Theme = "light" | "dark";

/* ─── Theme Context ─── */
interface ThemeContextValue {
  theme: Theme;
  toggleTheme: () => void;
}

const ThemeContext = createContext<ThemeContextValue>({
  theme: "dark",
  toggleTheme: () => {},
});

export function useTheme(): ThemeContextValue {
  return useContext(ThemeContext);
}

function ThemeProvider({ children }: { children: ReactNode }) {
  const [theme, setTheme] = useState<Theme>("dark");

  useThemeEffect(() => {
    const saved = localStorage.getItem("theme");
    if (saved === "light" || saved === "dark") {
      setTheme(saved);
      document.documentElement.setAttribute("data-theme", saved);
    }
  }, []);

  const toggleTheme = useCallback(() => {
    setTheme((current) => {
      const next: Theme = current === "dark" ? "light" : "dark";
      localStorage.setItem("theme", next);
      document.documentElement.setAttribute("data-theme", next);
      return next;
    });
  }, []);

  const value = useMemo<ThemeContextValue>(
    () => ({ theme, toggleTheme }),
    [theme, toggleTheme],
  );

  return <ThemeContext.Provider value={value}>{children}</ThemeContext.Provider>;
}

/* ─── Prevent FOUC (roda antes da hidratação) ─── */
function ThemeScript() {
  return (
    <script
      dangerouslySetInnerHTML={{
        __html: `
          (function() {
            try {
              var theme = localStorage.getItem('theme');
              if (theme !== 'light' && theme !== 'dark') {
                theme = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
              }
              document.documentElement.setAttribute('data-theme', theme);
            } catch(e) {}
          })();
        `,
      }}
    />
  );
}

/* ─── Google Fonts ─── */
export const links: Route.LinksFunction = () => [
  { rel: "icon", href: "/favicon.svg", type: "image/svg+xml" },
  { rel: "icon", href: "/favicon.ico", sizes: "any" },
  { rel: "apple-touch-icon", href: "/apple-touch-icon-180x180.png" },
  { rel: "preconnect", href: "https://fonts.googleapis.com" },
  {
    rel: "preconnect",
    href: "https://fonts.gstatic.com",
    crossOrigin: "anonymous",
  },
  {
    rel: "stylesheet",
    href: "https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@400;500;600;700&family=DM+Sans:ital,opsz,wght@0,9..40,100..1000;1,9..40,100..1000&display=swap",
  },
];

export function Layout({ children }: { children: ReactNode }) {
  return (
    <html lang="pt-BR" data-theme="dark" suppressHydrationWarning>
      <head>
        <meta charSet="utf-8" />
        <meta
          name="viewport"
          content="width=device-width, initial-scale=1, viewport-fit=cover"
        />
        <meta name="theme-color" content="#2563eb" />
        <meta name="description" content="Flashcards — gere e estude baralhos com repetição espaçada." />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-title" content="Flashcards" />
        <meta
          name="apple-mobile-web-app-status-bar-style"
          content="black-translucent"
        />
        {import.meta.env.PROD ? (
          <link rel="manifest" href="/manifest.webmanifest" />
        ) : null}
        <Meta />
        <Links />
        <ThemeScript />
      </head>
      <body className="min-h-screen bg-base-100 font-sans text-base-content antialiased transition-colors duration-300">
        <ThemeProvider>{children}</ThemeProvider>
        <ScrollRestoration />
        <Scripts />
      </body>
    </html>
  );
}

export default function App() {
  // Registra o service worker do PWA (apenas em produção, onde o
  // vite-plugin-pwa gera sw.js + manifest.webmanifest).
  useEffect(() => {
    if (import.meta.env.PROD && "serviceWorker" in navigator) {
      // updateViaCache: "none" — o navegador (em especial iOS Safari, que é
      // "pegajoso" com service worker) não pode usar o cache HTTP ao buscar o
      // sw.js, senão fica servindo um SW antigo com assets removidos do
      // servidor (tela branca no iPhone). Força a checagem a cada visita.
      navigator.serviceWorker.register("/sw.js", { updateViaCache: "none" });
    }
  }, []);

  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <Outlet />
      </AuthProvider>
    </QueryClientProvider>
  );
}

export function ErrorBoundary({ error }: Route.ErrorBoundaryProps) {
  let title = "Algo deu errado";
  let details = "Ocorreu um erro inesperado. Tente recarregar a página.";

  if (isRouteErrorResponse(error)) {
    if (error.status === 404) {
      title = "Página não encontrada";
      details = "A página que você procura não existe ou foi movida.";
    } else {
      title = `Erro ${error.status}`;
      details = error.statusText || details;
    }
  } else if (error instanceof Error && error.message) {
    details = error.message;
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-base-100 p-4">
      <div className="card w-full max-w-md border border-base-300 bg-base-100">
        <div className="card-body items-center text-center">
          <h1 className="card-title text-2xl">{title}</h1>
          <p className="text-base-content/70">{details}</p>
          <button
            type="button"
            className="btn btn-primary mt-4"
            onClick={() => window.location.reload()}
          >
            Recarregar página
          </button>
        </div>
      </div>
    </main>
  );
}
