import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import type { User } from "~/types/api";
import * as authService from "~/services/auth.service";
import { getApiErrorMessage } from "~/lib/api";
import {
  clearTokens,
  getAccessToken,
  getStoredUser,
  setStoredUser,
  setTokens,
} from "~/lib/auth";

export interface AuthContextValue {
  user: User | null;
  isAuthenticated: boolean;
  /** true depois de ler o storage no cliente (evita redirect precoce no SSR). */
  isReady: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  setUser: (user: User | null) => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  // Sempre inicia null para não divergir do HTML renderizado no servidor.
  const [user, setUserState] = useState<User | null>(null);
  const [isReady, setIsReady] = useState(false);

  useEffect(() => {
    if (getAccessToken()) {
      setUserState(getStoredUser());
    } else {
      setUserState(null);
    }
    setIsReady(true);
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    try {
      const response = await authService.login(email, password);
      setTokens(response.accessToken, response.refreshToken);
      setStoredUser(response.user);
      setUserState(response.user);
    } catch (error) {
      throw new Error(getApiErrorMessage(error));
    }
  }, []);

  const logout = useCallback(() => {
    clearTokens();
    setStoredUser(null);
    setUserState(null);
  }, []);

  const setUser = useCallback((next: User | null) => {
    setStoredUser(next);
    setUserState(next);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: !!user,
      isReady,
      login,
      logout,
      setUser,
    }),
    [user, isReady, login, logout, setUser],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth deve ser usado dentro de <AuthProvider>.");
  }
  return ctx;
}
