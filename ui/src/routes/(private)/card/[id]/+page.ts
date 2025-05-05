import type { PageLoad } from './$types';
import type { Card } from '$lib/types/card';
import { error } from '@sveltejs/kit';

export const load: PageLoad = async ({ fetch, params }) => {
	const response = await fetch(`/api/card/${params.id}`);
	if (response.ok) {
		const card = (await response.json()) as Card;
		const collectionCardResponse = await fetch(`/api/collectionStub/cards/${card.id}`);
		if (collectionCardResponse.ok) {
			const collectionEntry = (await collectionCardResponse.json()) as {
				cardId: number;
				amount: number;
			};
			return { card, collectionEntry };
		}
	} else {
		if (response.status === 401 || response.status === 403) {
			error(response.status, 'unauthorized');
		}
	}
	return {};
};
