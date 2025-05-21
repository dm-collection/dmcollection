<script lang="ts">
	import type { Card, CardFacet } from '$lib/types/card';
	import AmountButton from './AmountButton.svelte';
	import FacetProps from './FacetProps.svelte';
	let {
		card,
		collectionEntry
	}: { card: Card; collectionEntry: { cardId: number; amount: number } | undefined } = $props();
	let cards: Array<Array<CardFacet>> = $state([]);
	if (card.facets) {
		for (const [i, facet] of card.facets.entries()) {
			if (facet.imagePath || i == 0) {
				cards.push([facet]);
			} else {
				cards[cards.length - 1].push(facet);
			}
		}
	}
	async function onChange(newAmount: number) {
		try {
			const response = await fetch(`/api/collectionStub/cards/${card.id}`, {
				method: 'PUT',
				headers: {
					'Content-Type': 'application/json',
					'X-XSRF-TOKEN':
						document.cookie
							.split('; ')
							.find((row) => row.startsWith('XSRF-TOKEN='))
							?.split('=')[1] ?? ''
				},
				body: JSON.stringify({ amount: newAmount })
			});
			if (response.ok) {
				collectionEntry = (await response.json()) as { cardId: number; amount: number };
			}
		} catch (error) {
			console.error(error);
		}
	}
</script>

<div class="grid grid-cols-2 gap-8 xl:grid-cols-4">
	<div class="self-center justify-self-center xl:col-start-2">
		{#if card.idText || card.rarity}
			<div class="flex flex-row gap-1">
				<p class="text-lg font-semibold">{card.idText}</p>
				{#if card.rarity}
					<span
						class="p-y-1 inline-flex items-center rounded bg-white px-2 text-xs font-medium text-black ring-1 ring-inset ring-black/10"
						>{card.rarity}</span
					>
				{/if}
			</div>
		{/if}
	</div>
	<div class="justify-self-center">
		<AmountButton value={collectionEntry?.amount ?? 0} min={0} {onChange} />
	</div>
	{#each cards as facets (facets[0].position)}
		<div class="col-span-1 self-center justify-self-center xl:col-start-2">
			{#if facets[0]?.imagePath}
				<img
					src={facets[0].imagePath}
					alt="Image showing facet #{facets[0].position} of the card {card.dmId}"
				/>
			{/if}
		</div>
		<div class="col-span-1 flex flex-col gap-8">
			{#each facets as facet (facet.position)}
				<FacetProps {facet} />
			{/each}
		</div>
	{/each}
</div>
