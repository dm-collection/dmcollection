<script lang="ts">
	import { invalidate, replaceState } from '$app/navigation';
	import { page } from '$app/state';
	import CardFilters from '$lib/components/CardFilters.svelte';
	import CountedCardStub from '$lib/components/CountedCardStub.svelte';
	import PageNav from '$lib/components/PageNav.svelte';
	import { getRarities } from '$lib/rarity.svelte';
	import { getSets } from '$lib/sets.svelte';
	import { getSpecies } from '$lib/species.svelte';
	import type { CollectionData } from '$lib/types/collection';
	import type { PageProps } from './$types';
	import PencilSimple from 'phosphor-svelte/lib/PencilSimple';

	let { data }: PageProps = $props();

	let collection = $state(data.collection);

	let editDialog: HTMLDialogElement;

	let newName = $state('');

	async function showDialog() {
		editDialog?.showModal();
	}

	async function closeDialog() {
		editDialog?.close();
	}

	async function renameDeck() {
		newName = newName.trim();
		if (data.deck) {
			const response = await fetch(`/api/deck/${data.deck.id}`, {
				method: 'POST',
				headers: {
					'Content-Type': 'application/json',
					'X-XSRF-TOKEN':
						document.cookie
							.split('; ')
							.find((row) => row.startsWith('XSRF-TOKEN='))
							?.split('=')[1] ?? ''
				},
				body: JSON.stringify({ name: newName })
			});
			if (response.ok) {
				invalidate(`/api/deck/${data.deck.id}`);
				newName = '';
				closeDialog();
			} else {
				const input = document.getElementById('newName') as HTMLInputElement;
				input.classList.add('invalid:border-red-700');
				input.reportValidity();
			}
		}
	}

	async function runSearch(newParams: URLSearchParams, newPageNumber?: number) {
		try {
			let pageNumber = newPageNumber ?? 0;
			let url = new URL(page.url);
			url.hash = newParams.toString();
			replaceState(url, page.state);
			const response = await fetch(`/api/collection/${pageNumber}?${newParams.toString()}`);
			if (response.ok) {
				let newCollection = (await response.json()) as CollectionData;
				newCollection.cardPage.page.number += 1;
				collection = newCollection;
			}
		} catch (error) {
			console.error(error);
		}
	}

	async function changePage(newPageNum: number) {
		runSearch(data.search?.searchParams ?? new URLSearchParams(), newPageNum - 1);
	}

	async function amountChange(cardId: number, newAmount: number) {
		await data.deck?.setCardAmount(cardId, newAmount);
	}
</script>

<svelte:head>
	<title>{data.deck?.getInfo()?.name ?? 'DM Collection'}</title>
</svelte:head>

<dialog id="editDialog" class="m-auto rounded-md px-6 py-6 shadow-lg" bind:this={editDialog}>
	<label for="newName">Enter a new name for this deck:</label>
	<input
		id="newName"
		class="select ml-2"
		type="text"
		bind:value={newName}
		required
		aria-required="true"
		minlength="1"
		onkeyup={(e) => {
			if (e.key === 'Enter') {
				renameDeck();
			}
		}}
	/>
	<div class="mt-4 flex flex-row justify-between">
		<button class="btn-secondary" onclick={closeDialog}>Cancel</button>
		<button class="btn-primary" onclick={renameDeck}>Save</button>
	</div>
</dialog>
<div
	class="full-height-without-header flex min-h-full gap-4 overflow-hidden portrait:flex-col landscape:flex-row"
>
	{#if data.deck?.isLoaded() && collection}
		<div
			class="flex flex-col rounded-md bg-white p-2 shadow-md portrait:max-h-[41%] portrait:min-h-[41%] landscape:max-w-[40%] landscape:min-w-[40%]"
		>
			<div class="flex flex-row gap-4">
				<h1 class="txt-h1">Deck: {data.deck.getInfo()!.name}</h1>
				<button class="btn-secondary" onclick={showDialog}>
					<PencilSimple class="mr-1"></PencilSimple>Rename
				</button>
			</div>

			<p>Total Cards: {data.deck.getInfo()?.totalCardCount}</p>
			<div class="overflow-y-auto landscape:max-h-full">
				{#if data.deck.getCards().length > 0}
					<div
						class="grid gap-4 shadow-inner
						portrait:min-w-full portrait:md:grid-cols-5 portrait:lg:grid-cols-6 portrait:xl:grid-cols-7
						landscape:min-w-full landscape:lg:grid-cols-5 landscape:xl:grid-cols-6"
					>
						{#each data.deck.getCards() as card (card.id)}
							{@const inCollection = collection.cardPage.content.find(
								(c) => c.id == card.id
							)?.amount}
							<CountedCardStub
								{card}
								amount={card.amount}
								max={inCollection}
								onChange={(newAmount: number) => {
									amountChange(card.id, newAmount);
								}}
							/>
						{/each}
					</div>
				{:else}
					<p class="italic">This deck is empty. Add cards from your collection.</p>
				{/if}
			</div>
		</div>
		<div
			class="flex flex-col rounded-md bg-white p-2 shadow-md portrait:max-h-[58%] portrait:min-h-[58%] landscape:max-w-[59%] landscape:min-w-[59%]"
		>
			<h1 class="txt-h1">Your Collection</h1>
			{#await getSets() then sets}
				{#await getSpecies() then species}
					{#await getRarities() then rarities}
						<CardFilters
							search={data.search}
							{sets}
							{species}
							{rarities}
							changeCallback={runSearch}
						/>
					{/await}
				{/await}
			{/await}
			{#if collection.cardPage.page.totalPages > 1}
				<PageNav pageInfo={collection.cardPage.page} onForward={changePage} onBack={changePage} />
			{/if}
			<div class="overflow-y-auto landscape:max-h-full">
				{#if collection.cardPage.content.length > 0}
					<div
						class="grid gap-4 shadow-inner
						portrait:md:grid-cols-4 portrait:lg:grid-cols-5 portrait:xl:grid-cols-6
					    landscape:lg:grid-cols-4 landscape:xl:grid-cols-8"
					>
						{#each collection.cardPage.content as card (card.id)}
							{#await data.deck.getAmount(card.id) then inDeck}
								<CountedCardStub
									{card}
									amount={inDeck}
									max={card.amount}
									showMax={true}
									onChange={(newAmount: number) => {
										amountChange(card.id, newAmount);
									}}
								/>
							{/await}
						{/each}
					</div>
				{:else if data.search.isDefault()}
					<h1>Your collection is empty.</h1>
				{:else}
					<h1>No results. Try adjusting the filters or collect matching cards.</h1>
				{/if}
			</div>
		</div>
	{:else}
		<h1>NOT FOUND</h1>
	{/if}
</div>
