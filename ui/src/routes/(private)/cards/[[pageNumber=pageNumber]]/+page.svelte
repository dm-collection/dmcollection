<script lang="ts">
	import Pagination from '$lib/components/Pagination.svelte';
	import { goto } from '$app/navigation';
	import CardFilters from '$lib/components/CardFilters.svelte';
	import { getSets } from '$lib/sets.svelte';
	import { getSpecies } from '$lib/species.svelte';
	import { getRarities } from '$lib/rarity.svelte';
	import { api } from '$lib/api';
	import CountedPrintingStub from '$lib/components/CountedPrintingStub.svelte';
	import type { PrintingStub } from '$lib/types/card';
	import type { PageProps } from './$types';

	let { data = $bindable() }: PageProps = $props();

	async function runSearch(newParams: URLSearchParams) {
		await goto(`/cards?${newParams.toString()}`, { replaceState: true });
	}

	async function amountChange(
		printing: PrintingStub,
		cardIdx: number,
		printingIdx: number,
		newAmount: number
	) {
		try {
			const response = await api('/api/collectionStub', {
				method: 'PUT',
				json: { cardId: printing.id, amount: newAmount }
			});
			if (response.ok) {
				printing.amount = newAmount;
				if (data.cardPage) {
					data.cardPage.content[cardIdx].printings[printingIdx] = printing;
					data = data;
				}
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
				{#each card.printings as printing, j (printing.id)}
					<CountedPrintingStub
						{printing}
						amount={printing.amount}
						sizes="(width >= 80rem) calc((100vw - 7 * 2rem) / 8), (width >= 64rem) calc((100vw - 7 * 2rem) / 5), 100vw"
						onChange={(newAmount: number) => {
							amountChange(printing, i, j, newAmount);
						}}
					/>
				{/each}
			{/each}
		</div>
		<Pagination pageInfo={data.cardPage.page} path="/cards" />
	{:else}
		<p class="text-center">No results. Try adjusting the filters.</p>
	{/if}
{:else}
	<h1>NOT FOUND</h1>
{/if}
