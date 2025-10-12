<script lang="ts">
	import type { Card, CardFacet } from '$lib/types/card';
	import AmountButton from './AmountButton.svelte';
	import FacetProps from './FacetProps.svelte';
	import { goto } from '$app/navigation';
	import { invalidateAuth } from '$lib/auth.svelte';
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
			} else if (response.status === 401 || response.status === 403) {
				invalidateAuth();
				goto('/login');
			}
		} catch (error) {
			console.error(error);
		}
	}
</script>

<div class="flex flex-col gap-4">
	<div class="flex flex-row items-center justify-evenly">
		{#if card.idText || card.rarity}
			<div class="flex flex-row gap-1">
				<p class="text-lg font-semibold">{card.idText}</p>
				{#if card.rarity}
					<span
						class="p-y-1 inline-flex items-center rounded bg-white px-2 text-xs font-medium text-black ring-1 ring-black/10 ring-inset"
						>{card.rarity}</span
					>
				{/if}
			</div>
		{/if}
		<div>
			<AmountButton value={collectionEntry?.amount ?? 0} min={0} {onChange} />
		</div>
	</div>

	{#each cards as facets (facets[0].position)}
		<div class="mx-auto flex max-w-fit flex-wrap items-stretch justify-center gap-8">
			{#if facets[0]?.imagePath}
				<img
					onload={(event) => {
						const img = event.target as HTMLImageElement;
						if (img?.naturalWidth < 400) {
							img.classList.replace('min-w-[min(400px,100%)]', 'min-w-min');
						}
					}}
					src={facets[0].imagePath}
					alt="Image showing facet #{facets[0].position} of the card {card.dmId}"
					class="max-h-screen w-auto max-w-[min(500px,100%)] min-w-[min(400px,100%)] flex-shrink object-scale-down"
				/>
			{/if}
			<div class="max-w-prose flex-[30ch]">
				<div class={['flex', 'h-full', 'flex-col', 'justify-between', 'gap-8']}>
					{#each facets as facet (facet.position)}
						<FacetProps {facet} />
					{/each}
				</div>
			</div>
		</div>
	{/each}
</div>
