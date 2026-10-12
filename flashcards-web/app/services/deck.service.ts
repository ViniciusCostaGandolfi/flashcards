import { api } from "~/lib/api";
import type { Card, Deck } from "~/types/api";

export const listDecks = async (): Promise<Deck[]> => {
  const { data } = await api.get<Deck[]>("/decks");
  return data;
};

export const getDeck = async (id: string): Promise<Deck> => {
  const { data } = await api.get<Deck>(`/decks/${id}`);
  return data;
};

export const deleteDeck = async (id: string): Promise<void> => {
  await api.delete(`/decks/${id}`);
};

export const getDeckCards = async (
  id: string,
  tag?: string,
): Promise<Card[]> => {
  const { data } = await api.get<Card[]>(`/decks/${id}/cards`, {
    params: tag ? { tag } : undefined,
  });
  return data;
};
