let species: string[] = $state([]);

export async function getSpecies() {
	if (species.length > 0) {
		return species;
	}

	const setResponse = await fetch('/api/species');
	if (setResponse.ok) {
		species = (await setResponse.json()) as Array<string>;
		return species;
	}
}
