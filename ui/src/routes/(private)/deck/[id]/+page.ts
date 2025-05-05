import { Deck } from '$lib/Deck.svelte';
import { SearchFilter } from '$lib/SearchFilter.svelte';
import type { CollectionData } from '$lib/types/collection';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch, params }) => {
	try {
		const deck = new Deck(params.id);
		await deck.loadCollection(fetch);
		const fragment = window.location.hash.slice(1);
		let search = new SearchFilter();
		if (fragment.length > 0) {
			const searchParams = new URLSearchParams(fragment);
			search = new SearchFilter(searchParams);
		}
		const response = await fetch(`/api/collection/0?${search.searchParams.toString()}`); // load first page only
		if (response.ok) {
			const collection = (await response.json()) as CollectionData;
			if (collection && collection.cardPage?.page) {
				collection.cardPage.page.number += 1;
			}
			return { deck, collection, search };
		}
	} catch (error) {
		console.error(error);
	}
	return {};
};
