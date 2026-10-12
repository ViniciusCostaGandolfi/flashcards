import { api } from "~/lib/api";
import type { FileDetail, FileSummary } from "~/types/api";

export const listFiles = async (): Promise<FileSummary[]> => {
  const { data } = await api.get<FileSummary[]>("/files");
  return data;
};

export const getFile = async (id: string): Promise<FileDetail> => {
  const { data } = await api.get<FileDetail>(`/files/${id}`);
  return data;
};

/** Baixa o conteúdo binário do arquivo (imagem/PDF) como Blob. */
export const getFileContent = async (id: string): Promise<Blob> => {
  const { data } = await api.get<Blob>(`/files/${id}/content`, {
    responseType: "blob",
  });
  return data;
};
