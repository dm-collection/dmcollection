import type { Printing, CardStub } from '$lib/types/card';
import type { PagedResult } from '$lib/types/page';
import { apiFetch, type FetchFn } from './client';

export const fetchCardsPage = (fetch: FetchFn, page: number, params: URLSearchParams) =>
	apiFetch<PagedResult<CardStub>>(`/api/cards/${page}?${params.toString()}`, {}, fetch);

export const fetchCard = (fetch: FetchFn, id: string) =>
	apiFetch<Printing>(`/api/card/${id}`, {}, fetch);
