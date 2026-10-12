import { api } from "~/lib/api";
import type { LoginResponse } from "~/types/api";

export const login = async (
  email: string,
  password: string,
): Promise<LoginResponse> => {
  const { data } = await api.post<LoginResponse>("/auth/login", {
    email,
    password,
  });
  return data;
};

export const refresh = async (
  refreshToken: string,
): Promise<LoginResponse> => {
  const { data } = await api.post<LoginResponse>("/auth/refresh", {
    refreshToken,
  });
  return data;
};
