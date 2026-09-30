import type { DeckInfo } from '$lib/types/deck';
import type { PageLoad } from './$types';
import { error } from '@sveltejs/kit';

export const load: PageLoad = async ({ fetch }) => {
	const response = await fetch('/api/decks');
	if (response.ok) {
		const decks = (await response.json()) as Array<DeckInfo>;
		return { decks };
	} else if (response.status === 401 || response.status === 403) {
		error(response.status, 'unauthorized');
	}
	return {};
};
