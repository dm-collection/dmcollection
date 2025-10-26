import { Deck } from '$lib/Deck.svelte';
import { SearchFilter } from '$lib/SearchFilter.svelte';
import type { CardStub } from '$lib/types/card';
import type { CollectionData } from '$lib/types/collection';
import type { PagedResult } from '$lib/types/page';
import type { PageLoad } from './$types';
import { error } from '@sveltejs/kit';

export const load: PageLoad = async ({ fetch, params }) => {
	const deck = new Deck(params.id);
	await deck.loadCollection(fetch);
	const fragment = window.location.hash.slice(1);
	let search = new SearchFilter();
	let ownedOnly = false;
	if (fragment.length > 0) {
		const searchParams = new URLSearchParams(fragment);
		ownedOnly = searchParams.get('ownedOnly') === 'true';
		searchParams.delete('ownedOnly'); // Remove before passing to SearchFilter
		search = new SearchFilter(searchParams);
	}

	const endpoint = ownedOnly ? 'collection' : 'cards';
	const response = await fetch(`/api/${endpoint}/0?${search.searchParams.toString()}`);
	if (response.ok) {
		let cardPage: PagedResult<CardStub>;
		if (ownedOnly) {
			const collection = (await response.json()) as CollectionData;
			cardPage = collection.cardPage;
		} else {
			cardPage = (await response.json()) as PagedResult<CardStub>;
		}
		if (cardPage?.page) {
			cardPage.page.number += 1;
		}
		return { deck, cardPage, search, ownedOnly };
	} else if (response.status === 401 || response.status === 403) {
		error(response.status, 'unauthorized');
	}
	return {};
};
