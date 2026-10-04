import type { PageLoad } from './$types';
import type { Printing } from '$lib/types/card';
import { error } from '@sveltejs/kit';

export const load: PageLoad = async ({ fetch, params }) => {
	const response = await fetch(`/api/card/${params.id}`);
	if (response.ok) {
		const printing = (await response.json()) as Printing;
		const collectionCardResponse = await fetch(`/api/collectionStub/printings/${printing.id}`);
		if (collectionCardResponse.ok) {
			const collectionEntry = (await collectionCardResponse.json()) as {
				printingId: number;
				amount: number;
			};
			return { printing: printing, collectionEntry };
		}
	} else if (response.status === 401 || response.status === 403) {
		error(response.status, 'unauthorized');
	}

	return {};
};
