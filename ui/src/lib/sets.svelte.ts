import type { CardSet } from './types/card';

let sets: CardSet[] = $state([]);

export async function getSets() {
	if (sets.length > 0) {
		return sets;
	}

	const setResponse = await fetch('/api/sets');
	if (setResponse.ok) {
		sets = (await setResponse.json()) as Array<CardSet>;
		return sets;
	}
}
