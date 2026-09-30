import type { CollectionData } from '$lib/types/collection';
import { apiFetch, type FetchFn } from './client';

export interface CollectionStubEntry {
	cardId: number;
	amount: number;
}

export const fetchCollectionPage = (fetch: FetchFn, page: number, params: URLSearchParams) =>
	apiFetch<CollectionData>(`/api/collection/${page}?${params.toString()}`, {}, fetch);

export const fetchCollectionStub = (fetch: FetchFn, cardId: number) =>
	apiFetch<CollectionStubEntry>(`/api/collectionStub/cards/${cardId}`, {}, fetch);

export const setCardCount = (cardId: number, amount: number) =>
	apiFetch<void>('/api/collectionStub', {
		method: 'PUT',
		json: { cardId, amount },
		parseJson: false
	});

export const setCardCountAt = (cardId: number, amount: number) =>
	apiFetch<CollectionStubEntry>(`/api/collectionStub/cards/${cardId}`, {
		method: 'PUT',
		json: { amount }
	});

export const importCollection = (bytes: BodyInit) =>
	apiFetch<void>('/api/collection/import', { binary: bytes, parseJson: false });
