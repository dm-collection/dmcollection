import type { PageLoad } from './$types';
import type { CollectionData } from '$lib/types/collection';
import { SearchFilter } from '$lib/SearchFilter.svelte';
import { error } from '@sveltejs/kit';

export const load: PageLoad = async ({ fetch, params, url }) => {
	const pageNumber = params.pageNumber ? parseFloat(params.pageNumber) - 1 : 0; // server-side pages are 0-indexed
	const search = new SearchFilter(url.searchParams);
	const response = await fetch(`/api/collection/${pageNumber}?${search.searchParams.toString()}`);
	if (response.ok) {
		const collection = (await response.json()) as CollectionData;
		if (collection?.cardPage?.page) {
			collection.cardPage.page.number += 1;
		}
		return { collection, search };
	} else if (response.status === 401 || response.status === 403) {
		error(response.status, 'unauthorized');
	}
	return { search };
};
