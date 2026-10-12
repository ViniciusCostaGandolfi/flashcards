import { api } from "~/lib/api";
import type {
  CreateGenerationInput,
  CreateGenerationResponse,
  GenerationJob,
} from "~/types/api";

/**
 * Cria um job de geração. Envia `multipart/form-data` com EITHER `file`
 * (imagem/PDF) OR `text`, um `title` opcional e `tags` opcionais
 * (nomes separados por vírgula) aplicadas a todos os cartões gerados.
 * Não definimos Content-Type manualmente: o axios/browser anexa o boundary.
 */
export const createGeneration = async (
  input: CreateGenerationInput,
): Promise<CreateGenerationResponse> => {
  const formData = new FormData();

  if (input.file) {
    formData.append("file", input.file);
  }
  if (input.text?.trim()) {
    formData.append("text", input.text.trim());
  }
  if (input.title?.trim()) {
    formData.append("title", input.title.trim());
  }
  const tags = (input.tags ?? [])
    .map((tag) => tag.trim())
    .filter(Boolean);
  if (tags.length > 0) {
    formData.append("tags", tags.join(","));
  }

  const { data } = await api.post<CreateGenerationResponse>(
    "/generations",
    formData,
  );
  return data;
};

export const getGeneration = async (
  id: string,
): Promise<GenerationJob> => {
  const { data } = await api.get<GenerationJob>(`/generations/${id}`);
  return data;
};
