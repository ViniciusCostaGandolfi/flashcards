import { api } from "~/lib/api";
import type { Card, CardInput, ReviewGrade, Tag } from "~/types/api";

export const createCard = async (input: CardInput): Promise<Card> => {
  const { data } = await api.post<Card>("/cards", input);
  return data;
};

export const updateCard = async (
  id: string,
  input: Partial<CardInput>,
): Promise<Card> => {
  const { data } = await api.put<Card>(`/cards/${id}`, input);
  return data;
};

export const deleteCard = async (id: string): Promise<void> => {
  await api.delete(`/cards/${id}`);
};

export const reviewCard = async (
  id: string,
  grade: ReviewGrade,
): Promise<Card> => {
  const { data } = await api.post<Card>(`/cards/${id}/review`, { grade });
  return data;
};

/* ───────────────────────────── Tags ───────────────────────────── */

export const listTags = async (): Promise<Tag[]> => {
  const { data } = await api.get<Tag[]>("/tags");
  return data;
};

export const createTag = async (name: string): Promise<Tag> => {
  const { data } = await api.post<Tag>("/tags", { name });
  return data;
};

export const deleteTag = async (id: string): Promise<void> => {
  await api.delete(`/tags/${id}`);
};
