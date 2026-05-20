import type { HistoryEntry } from '$lib/types/history';
import { api, type FetchFn } from '../api';

export const loadLatest = async (fetch: FetchFn, limit: number) => {
	const response = await api(`/api/history/latest?limit=${limit}`, { fetchFn: fetch });
	if (response.ok) {
		return response.json() as Promise<HistoryEntry[]>;
	} else {
		throw new Error(response.statusText);
	}
};
