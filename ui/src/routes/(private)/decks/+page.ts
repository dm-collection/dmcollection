import type { CollectionInfo } from '$lib/types/collection';
import type { PageLoad } from './$types';

export const load: PageLoad = async ({ fetch }) => {
	try {
		const response = await fetch('/api/decks');
		if (response.ok) {
			const decks = (await response.json()) as Array<CollectionInfo>;
			return { decks };
		}
	} catch (error) {
		console.error(error);
	}
	return {};
};
