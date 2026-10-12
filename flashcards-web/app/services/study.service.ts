import { api } from "~/lib/api";
import type { Card } from "~/types/api";

/**
 * Cartões devidos. Sem `deckId` retorna de todos os baralhos.
 * `tag` filtra por nome de tag.
 */
export const getDueCards = async (
  deckId?: string,
  tag?: string,
): Promise<Card[]> => {
  const params: Record<string, string> = {};
  if (deckId) params.deckId = deckId;
  if (tag) params.tag = tag;

  const { data } = await api.get<Card[]>("/study/due", {
    params: Object.keys(params).length > 0 ? params : undefined,
  });
  return data;
};
