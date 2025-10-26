<script lang="ts">
	import { goto, invalidate, replaceState } from '$app/navigation';
	import { page } from '$app/state';
	import CardFilters from '$lib/components/CardFilters.svelte';
	import CountedCardStub from '$lib/components/CountedCardStub.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { getRarities } from '$lib/rarity.svelte';
	import { getSets } from '$lib/sets.svelte';
	import { getSpecies } from '$lib/species.svelte';
	import type { CardStub } from '$lib/types/card';
	import type { PagedResult } from '$lib/types/page';
	import { SvelteURL, SvelteURLSearchParams } from 'svelte/reactivity';
	import type { PageProps } from './$types';
	import PencilSimple from 'phosphor-svelte/lib/PencilSimple';
	import { invalidateAuth } from '$lib/auth.svelte';

	let { data }: PageProps = $props();

	let cardPage = $state(data.cardPage);
	let ownedOnly = $state(data.ownedOnly ?? false);

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
			} else if (response.status === 401 || response.status === 403) {
				invalidateAuth();
				goto('/login');
			} else {
				const input = document.getElementById('newName') as HTMLInputElement;
				input.classList.add('invalid:border-red-700');
				input.reportValidity();
			}
		}
	}

	async function runSearch(
		newParams: URLSearchParams,
		showOwnedOnly?: boolean,
		newPageNumber?: number
	) {
		try {
			let pageNumber = newPageNumber ?? 0;
			let url = new SvelteURL(page.url);
			const hashParams = new SvelteURLSearchParams(newParams);
			if (showOwnedOnly) {
				hashParams.set('ownedOnly', 'true');
			}
			url.hash = hashParams.toString();
			replaceState(url, page.state);

			const endpoint = showOwnedOnly ? 'collection' : 'cards';
			const response = await fetch(`/api/${endpoint}/${pageNumber}?${newParams.toString()}`);

			if (response.ok) {
				let result = await response.json();
				let newCardPage: PagedResult<CardStub>;

				if (showOwnedOnly) {
					// /api/collection returns CollectionData
					newCardPage = result.cardPage;
				} else {
					// /api/cards returns PagedResult directly
					newCardPage = result;
				}

				newCardPage.page.number += 1;
				cardPage = newCardPage;
			} else if (response.status === 401 || response.status === 403) {
				invalidateAuth();
				goto('/login');
			}
		} catch (error) {
			console.error(error);
		}
	}

	async function changePage(newPageNum: number) {
		runSearch(data.search?.searchParams ?? new URLSearchParams(), ownedOnly, newPageNum - 1);
	}

	async function amountChange(cardId: number, newAmount: number) {
		try {
			await data.deck?.setCardAmount(cardId, newAmount);
		} catch (err) {
			if (err instanceof Error && err.message === 'unauthorized') {
				invalidateAuth();
				goto('/login');
			}
		}
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
	{#if data.deck?.isLoaded() && cardPage}
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
							{@const inCollection = cardPage.content.find((c) => c.id == card.id)?.amount}
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
			<h1 class="txt-h1">{ownedOnly ? 'Your Collection' : 'All Cards'}</h1>
			{#await getSets() then sets}
				{#await getSpecies() then species}
					{#await getRarities() then rarities}
						<CardFilters
							search={data.search}
							{sets}
							{species}
							{rarities}
							changeCallback={runSearch}
							showOwnedOnlyToggle={true}
							bind:ownedOnly
						/>
					{/await}
				{/await}
			{/await}
			{#if cardPage.page.totalPages > 1}
				<Pagination pageInfo={cardPage.page} onForward={changePage} onBack={changePage} />
			{/if}
			<div class="overflow-y-auto landscape:max-h-full">
				{#if cardPage.content.length > 0}
					<div
						class="grid gap-4 shadow-inner
						portrait:md:grid-cols-4 portrait:lg:grid-cols-5 portrait:xl:grid-cols-6
					    landscape:lg:grid-cols-4 landscape:xl:grid-cols-8"
					>
						{#each cardPage.content as card (card.id)}
							{#await data.deck.getAmount(card.id) then inDeck}
								<CountedCardStub
									{card}
									amount={inDeck}
									max={card.amount}
									showMax={true}
									enforceMax={false}
									onChange={(newAmount: number) => {
										amountChange(card.id, newAmount);
									}}
								/>
							{/await}
						{/each}
					</div>
					{#if cardPage.page.totalPages > 1}
						<Pagination pageInfo={cardPage.page} onForward={changePage} onBack={changePage} />
					{/if}
				{:else if data.search.isDefault()}
					<h1>{ownedOnly ? 'Your collection is empty.' : 'No cards found.'}</h1>
				{:else}
					<h1>
						{ownedOnly
							? 'No results. Try adjusting the filters or collect matching cards.'
							: 'No results. Try adjusting the filters.'}
					</h1>
				{/if}
			</div>
		</div>
	{:else}
		<h1>NOT FOUND</h1>
	{/if}
</div>
