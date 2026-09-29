<script lang="ts">
	import Pagination from '$lib/components/Pagination.svelte';
	import { goto } from '$app/navigation';
	import CardFilters from '$lib/components/CardFilters.svelte';
	import { getSets } from '$lib/sets.svelte';
	import { getSpecies } from '$lib/species.svelte';
	import { getRarities } from '$lib/rarity.svelte';
	import { api } from '$lib/api';
	import CountedPrintingStub from '$lib/components/CountedPrintingStub.svelte';
	import type { CardStub, PrintingStub } from '$lib/types/card';
	import type { PageProps } from './$types';
	import ExpandingCardStub from '$lib/components/ExpandingCardStub.svelte';
	import { SvelteSet } from 'svelte/reactivity';
	import CaretUpIcon from 'phosphor-svelte/lib/CaretUpIcon';

	let { data }: PageProps = $props();

	// svelte-ignore state_referenced_locally
	// eslint-disable-next-line svelte/prefer-writable-derived
	let cards = $state(data.cardPage?.content);

	$effect(() => {
		cards = data.cardPage?.content;
	});

	let expanded = new SvelteSet<CardStub>();

	async function runSearch(newParams: URLSearchParams) {
		await goto(`/cards?${newParams.toString()}`, { replaceState: true });
	}

	async function expand(card: CardStub) {
		expanded.add(card);
	}

	async function collapse(card: CardStub) {
		expanded.delete(card);
	}

	async function amountChange(printing: PrintingStub, newAmount: number) {
		try {
			const response = await api('/api/collectionStub', {
				method: 'PUT',
				json: { cardId: printing.id, amount: newAmount }
			});
			if (response.ok) {
				printing.amount = newAmount;
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
{#if cards && cards.length > 0 && data.cardPage}
	<Pagination pageInfo={data.cardPage.page} path="/cards" />
	<div class="grid gap-6 lg:grid-cols-5 xl:grid-cols-8">
		{#each cards as card (card.id)}
			{#if card.printings.length > 1 && cards.length > 1}
				{#if expanded.has(card)}
					<div
						class="col-span-full -m-1 grid gap-6 rounded-lg border border-teal-700 p-1 inset-shadow-sm lg:grid-cols-5 xl:grid-cols-8"
					>
						<button
							class="col-span-full -mb-6 flex flex-row items-center justify-center text-sm"
							onclick={() => collapse(card)}
						>
							{card.name}
						</button>
						{#each card.printings as printing (printing.id)}
							<CountedPrintingStub
								{printing}
								amount={printing.amount}
								sizes="(width >= 80rem) calc((100vw - 7 * 2rem) / 8), (width >= 64rem) calc((100vw - 7 * 2rem) / 5), 100vw"
								onChange={(newAmount: number) => {
									amountChange(printing, newAmount);
								}}
							/>
						{/each}
						<button
							class="col-span-full -mt-6 flex flex-row items-center justify-center text-sm"
							onclick={() => collapse(card)}
						>
							<CaretUpIcon size="1em"></CaretUpIcon>
							Collapse
						</button>
					</div>
				{:else}
					<ExpandingCardStub
						{card}
						amount={card.printings.reduce((s, p) => s + p.amount, 0)}
						sizes="(width >= 80rem) calc((100vw - 7 * 2rem) / 8), (width >= 64rem) calc((100vw - 7 * 2rem) / 5), 100vw"
						onClick={() => {
							expand(card);
						}}
					/>
				{/if}
			{:else}
				{#each card.printings as printing (printing.id)}
					<CountedPrintingStub
						{printing}
						amount={printing.amount}
						sizes="(width >= 80rem) calc((100vw - 7 * 2rem) / 8), (width >= 64rem) calc((100vw - 7 * 2rem) / 5), 100vw"
						onChange={(newAmount: number) => {
							amountChange(printing, newAmount);
						}}
					/>
				{/each}
			{/if}
		{/each}
	</div>
	<Pagination pageInfo={data.cardPage.page} path="/cards" />
{:else}
	<p class="text-center">No results. Try adjusting the filters.</p>
{/if}
