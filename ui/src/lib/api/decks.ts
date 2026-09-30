import type { CollectionData, CollectionInfo } from '$lib/types/collection';
import { apiFetch, type FetchFn } from './client';

export const fetchDecks = (fetch: FetchFn) => apiFetch<CollectionInfo[]>('/api/decks', {}, fetch);

export const fetchDeck = (fetch: FetchFn, id: string) =>
	apiFetch<CollectionData>(`/api/deck/${id}`, {}, fetch);

export const createDeck = (name: string) =>
	apiFetch<CollectionInfo>('/api/decks', { json: { name } });

export const renameDeck = (id: string, name: string) =>
	apiFetch<void>(`/api/deck/${id}`, { method: 'POST', json: { name }, parseJson: false });

export const deleteDeck = (id: string) => apiFetch<void>(`/api/deck/${id}`, { method: 'DELETE' });

export const setDeckCardAmount = (deckId: string, cardId: number, amount: number) =>
	apiFetch<CollectionInfo>(`/api/deck/${deckId}/cards/${cardId}`, {
		method: 'PUT',
		json: { amount }
	});

export const importDecks = (bytes: BodyInit) =>
	apiFetch<void>('/api/decks/import', { binary: bytes, parseJson: false });
