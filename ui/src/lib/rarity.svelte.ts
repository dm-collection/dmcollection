import type { Rarity } from './types/rarity';

let rarities: Rarity[] = $state([]);

export async function getRarities() {
	if (rarities.length > 0) {
		return rarities;
	}

	const setResponse = await fetch('/api/rarities');
	if (setResponse.ok) {
		rarities = (await setResponse.json()) as Array<Rarity>;
		rarities.sort((a, b) => b.order - a.order);
		return rarities;
	}
}
