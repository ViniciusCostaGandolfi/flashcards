import { api } from "~/lib/api";
import type { ModelInfo, Settings } from "~/types/api";

export const getSettings = async (): Promise<Settings> => {
  const { data } = await api.get<Settings>("/settings");
  return data;
};

export const updateSettings = async (settings: Settings): Promise<Settings> => {
  const { data } = await api.put<Settings>("/settings", settings);
  return data;
};

export const listModels = async (): Promise<ModelInfo[]> => {
  const { data } = await api.get<ModelInfo[]>("/models");
  return data;
};
