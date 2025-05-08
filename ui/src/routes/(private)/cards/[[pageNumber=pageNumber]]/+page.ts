import type { PageLoad } from './$types';
import type { CardStub } from '$lib/types/card';
import type { PagedResult } from '$lib/types/page';
import { SearchFilter } from '$lib/SearchFilter.svelte';

export const load: PageLoad = async ({ fetch, params, url }) => {
	try {
		const pageNumber = params.pageNumber ? parseFloat(params.pageNumber) - 1 : 0; // server-side pages are 0-indexed
		const search = new SearchFilter(url.searchParams);
		const response = await fetch(`/api/cards/${pageNumber}?${search.searchParams.toString()}`);
		if (response.ok) {
			const cardPage = (await response.json()) as PagedResult<CardStub>;
			if (cardPage?.page) {
				cardPage.page.number += 1; // we index pages from 1 for easier display
			}
			return { cardPage, search };
		}
	} catch (error) {
		console.error(error);
	}
	return { search: new SearchFilter(url.searchParams) };
};
