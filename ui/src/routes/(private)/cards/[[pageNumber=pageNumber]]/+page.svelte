<script lang="ts">
	import Pagination from '$lib/components/Pagination.svelte';
	import type { PageData } from './$types';
	import { goto, invalidate } from '$app/navigation';
	import { type CardStub } from '$lib/types/card';
	import CountedCardStub from '$lib/components/CountedCardStub.svelte';
	import CardFilters from '$lib/components/CardFilters.svelte';
	import { getSets } from '$lib/sets.svelte';
	import { getSpecies } from '$lib/species.svelte';
	import { getRarities } from '$lib/rarity.svelte';
	import { invalidateAuth } from '$lib/auth.svelte';

	let { data }: { data: PageData } = $props();

	async function runSearch(newParams: URLSearchParams) {
		await goto(`/cards?${newParams.toString()}`, { replaceState: true });
	}

	async function amountChange(card: CardStub, i: number, newAmount: number) {
		try {
			const response = await fetch('/api/collectionStub', {
				method: 'PUT',
				headers: {
					'Content-Type': 'application/json',
					'X-XSRF-TOKEN':
						document.cookie
							.split('; ')
							.find((row) => row.startsWith('XSRF-TOKEN='))
							?.split('=')[1] ?? ''
				},
				body: JSON.stringify({ cardId: card.id, amount: newAmount })
			});
			if (response.ok) {
				card.amount = newAmount;
				if (data.cardPage) {
					data.cardPage.content[i] = card;
					data = data;
					invalidate((url) => url.pathname.startsWith('/api/cards'));
				}
			} else if (response.status === 401 || response.status === 403) {
				invalidateAuth();
				goto('/login');
			}
		} catch (error) {
			console.error(error);
		}
	}
</script>

<svelte:head>
	<title>Cards</title>
</svelte:head>

<h1 class="txt-h1">{data.cardPage?.page.totalElements} Cards</h1>
{#await getSets() then sets}
	{#await getSpecies() then species}
		{#await getRarities() then rarities}
			<CardFilters search={data.search} {sets} {species} {rarities} changeCallback={runSearch} />
		{/await}
	{/await}
{/await}
{#if data.cardPage != undefined}
	{#if data.cardPage.content.length > 0}
		<Pagination pageInfo={data.cardPage.page} path="/cards" />
		<div class="grid gap-8 lg:grid-cols-5 xl:grid-cols-8">
			{#each data.cardPage.content as card, i (card.id)}
				<CountedCardStub
					{card}
					amount={card.amount}
					onChange={(newAmount: number) => {
						amountChange(card, i, newAmount);
					}}
				/>
			{/each}
		</div>
		<Pagination pageInfo={data.cardPage.page} path="/cards" />
	{:else}
		<p class="text-center">No results. Try adjusting the filters.</p>
	{/if}
{:else}
	<h1>NOT FOUND</h1>
{/if}
