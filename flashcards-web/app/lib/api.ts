import axios, {
  type AxiosError,
  type AxiosInstance,
  type InternalAxiosRequestConfig,
} from "axios";
import type { LoginResponse } from "~/types/api";
import {
  clearTokens,
  getAccessToken,
  getRefreshToken,
  setStoredUser,
  setTokens,
} from "./auth";

/**
 * Corpo padrão de erro da API (record ApiError):
 * { status, message, timestamp, errors }.
 */
interface ApiErrorResponse {
  status: number;
  message: string;
  timestamp: string;
  errors?: string[] | null;
}

/**
 * Normaliza a baseURL da API. Os services chamam paths relativos como
 * /auth/login, então a baseURL precisa terminar com /api/v1. Aceita:
 *  - VITE_API_URL vazio/ausente → "/api/v1"
 *  - VITE_API_URL="https://api.exemplo.com" → "https://api.exemplo.com/api/v1"
 *  - VITE_API_URL="https://api.exemplo.com/api/v1" → inalterado
 *  - VITE_API_URL="https://api.exemplo.com/" → "https://api.exemplo.com/api/v1"
 */
function resolveBaseUrl(): string {
  const raw = import.meta.env.VITE_API_URL;
  if (!raw || raw.trim() === "") return "/api/v1";
  const url = raw.replace(/\/+$/, "");
  if (url.endsWith("/api/v1")) return url;
  return `${url}/api/v1`;
}

const baseURL = resolveBaseUrl();

export function getBaseUrl(): string {
  return baseURL;
}

// NÃO definir "Content-Type" global aqui: o transformRequest do axios, ao ver
// um Content-Type contendo "application/json" junto de um FormData, converte o
// FormData em JSON (formDataToJSON) e envia application/json — quebrando o
// upload multipart de /generations. Sem header global, o axios define
// application/json para objetos e multipart (com boundary) para FormData.
export const api: AxiosInstance = axios.create({
  baseURL,
  timeout: 60_000,
});

api.interceptors.request.use((config) => {
  const accessToken = getAccessToken();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

let isRefreshing = false;
let pendingQueue: Array<{
  resolve: () => void;
  reject: (reason?: unknown) => void;
}> = [];

const isAuthUrl = (url?: string): boolean =>
  !!url && (url.includes("/auth/login") || url.includes("/auth/refresh"));

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined;

    if (
      error.response?.status === 401 &&
      originalRequest &&
      !originalRequest._retry &&
      !isAuthUrl(originalRequest.url)
    ) {
      originalRequest._retry = true;

      // Single-flight: se outra requisição já está renovando o token, aguarda.
      if (isRefreshing) {
        return new Promise<void>((resolve, reject) => {
          pendingQueue.push({ resolve, reject });
        }).then(() => api(originalRequest));
      }

      isRefreshing = true;
      const refreshToken = getRefreshToken();

      if (!refreshToken) {
        clearTokens();
        setStoredUser(null);
        if (typeof window !== "undefined") window.location.assign("/login");
        return Promise.reject(error);
      }

      try {
        // axios puro (sem interceptors) para evitar recursão.
        const { data } = await axios.post<LoginResponse>(
          `${baseURL}/auth/refresh`,
          { refreshToken },
        );

        setTokens(data.accessToken, data.refreshToken);
        setStoredUser(data.user);
        pendingQueue.forEach(({ resolve }) => resolve());
        pendingQueue = [];

        // Reexecuta a requisição original uma vez (o interceptor de request
        // anexa o novo access token).
        return api(originalRequest);
      } catch (refreshError) {
        pendingQueue.forEach(({ reject }) => reject(refreshError));
        pendingQueue = [];
        clearTokens();
        setStoredUser(null);
        if (typeof window !== "undefined") window.location.assign("/login");
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  },
);

/** Extrai uma mensagem legível de um erro do axios. */
export function getApiErrorMessage(error: unknown): string {
  if (axios.isAxiosError<ApiErrorResponse>(error)) {
    const data = error.response?.data;

    if (data) {
      const message = typeof data.message === "string" ? data.message.trim() : "";
      if (message) {
        const details = data.errors?.filter((e) => !!e?.trim()).join(" · ");
        return details ? `${message} — ${details}` : message;
      }
    }

    if (error.code === "ECONNABORTED") {
      return "A requisição excedeu o tempo limite. Tente novamente.";
    }

    if (!error.response) {
      return "Não foi possível conectar ao servidor. Verifique sua conexão.";
    }

    return `Ocorreu um erro inesperado (HTTP ${error.response.status}). Tente novamente.`;
  }

  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Erro inesperado. Tente novamente.";
}
