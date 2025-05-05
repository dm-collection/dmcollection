import type { PageLoad } from './$types';
import type { CollectionData } from '$lib/types/collection';
import { SearchFilter } from '$lib/SearchFilter.svelte';

export const load: PageLoad = async ({ fetch, params, url }) => {
	try {
		const pageNumber = params.pageNumber ? parseFloat(params.pageNumber) - 1 : 0; // server-side pages are 0-indexed
		const search = new SearchFilter(url.searchParams);
		const response = await fetch(`/api/collection/${pageNumber}?${search.searchParams.toString()}`);
		if (response.ok) {
			const collection = (await response.json()) as CollectionData;
			if (collection && collection.cardPage?.page) {
				collection.cardPage.page.number += 1;
			}
			return { collection, search };
		}
	} catch (error) {
		console.error(error);
	}
	return { search: new SearchFilter(url.searchParams) };
};
