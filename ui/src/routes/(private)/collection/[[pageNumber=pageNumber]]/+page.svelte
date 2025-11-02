<script lang="ts">
	import { goto, invalidate } from '$app/navigation';
	import CardFilters from '$lib/components/CardFilters.svelte';
	import CountedCardStub from '$lib/components/CountedCardStub.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { getRarities } from '$lib/rarity.svelte';
	import { getSets } from '$lib/sets.svelte';
	import { getSpecies } from '$lib/species.svelte';
	import type { CardStub } from '$lib/types/card';
	import type { PageProps } from './$types';
	import DownloadSimple from 'phosphor-svelte/lib/DownloadSimple';
	import UploadSimple from 'phosphor-svelte/lib/UploadSimple';
	import Warning from 'phosphor-svelte/lib/Warning';
	import CircleNotch from 'phosphor-svelte/lib/CircleNotch';
	import { invalidateAuth } from '$lib/auth.svelte';
	let { data = $bindable() }: PageProps = $props();

	let editingEnabled: boolean = $state(false);

	let importDialog: HTMLDialogElement;
	let importFiles: FileList | null = $state(null);
	let uploading = $state(false);
	let uploadError = $state(false);

	async function showDialog() {
		importDialog?.showModal();
	}

	async function closeDialog() {
		importDialog?.close();
	}

	async function runSearch(newParams: URLSearchParams) {
		await goto(`/collection?${newParams.toString()}`, { replaceState: true });
	}

	async function startImport() {
		if (importFiles && importFiles.length > 0) {
			uploadError = false;
			uploading = true;
			const success = await importCollection();
			if (!success) {
				uploadError = true;
				uploading = false;
			} else {
				uploading = false;
				closeDialog();
				invalidate((url) => url.pathname.startsWith('/api/collection/'));
			}
		}
	}

	function handleDialogClose(e: Event) {
		if (uploading) {
			e.preventDefault();
		}
	}

	async function importCollection() {
		if (importFiles) {
			try {
				const fileBytes = await importFiles[0].arrayBuffer();

				const response = await fetch('/api/collection/import', {
					method: 'POST',
					headers: {
						'Content-Type': 'application/octet-stream',
						'X-XSRF-TOKEN':
							document.cookie
								.split('; ')
								.find((row) => row.startsWith('XSRF-TOKEN='))
								?.split('=')[1] ?? ''
					},
					body: fileBytes
				});
				if (response.status === 401 || response.status === 403) {
					invalidateAuth();
					goto('/login');
					return false;
				}
				return response.ok;
			} catch (error) {
				console.error('Error importing collection', error);
				return false;
			}
		}
	}

	async function amountChange(card: CardStub, i: number, newAmount: number) {
		const response = await fetch(`/api/collectionStub`, {
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
			if (data.collection) {
				data.collection.cardPage.content[i] = card;
				data = data;
				invalidate((url) => url.pathname.startsWith('/api/cards'));
			}
		} else if (response.status === 401 || response.status === 403) {
			invalidateAuth();
			goto('/login');
		}
	}
</script>

<svelte:head>
	<title>Collection</title>
</svelte:head>

<dialog
	id="importDialog"
	class="m-auto rounded-md px-6 py-6 shadow-lg"
	class:cursor-progress={uploading}
	bind:this={importDialog}
	oncancel={handleDialogClose}
>
	<div class="flex flex-col gap-4">
		<h1 class="txt-h1">Collection import</h1>
		<div class="flex flex-row items-center">
			<Warning size="2em" weight="bold" class="mr-2"></Warning>
			<p>
				The imported collection will overwrite your current one.<br />
				Make sure you have backed up your current collection.
			</p>
		</div>
		<div class="flex flex-col gap-2">
			<label for="import">Select a collection to import:</label>
			<input
				type="file"
				class="file:rounded-md file:border file:bg-white file:px-3 file:py-2 enabled:cursor-pointer enabled:file:border-teal-700 enabled:file:text-teal-700 enabled:hover:file:bg-teal-700 enabled:hover:file:text-teal-50 disabled:text-slate-300 disabled:file:border-slate-300"
				id="import"
				name="import"
				accept="application/json,text/json,.json"
				disabled={uploading}
				bind:files={importFiles}
				onchange={() => (uploadError = false)}
			/>
			<p class:hidden={!uploadError} class="font-bold text-red-700">
				Import from provided file failed.
			</p>
		</div>
		<div class="mt-4 flex flex-row justify-between">
			<button class="btn-secondary" onclick={closeDialog} disabled={uploading}>Cancel</button>
			<button
				class="btn-primary"
				class:cursor-progress={uploading}
				onclick={startImport}
				disabled={uploading || importFiles == null || importFiles.length == 0}
			>
				{#if uploading}
					<CircleNotch class="animate-spin"></CircleNotch>
				{/if}
				Import</button
			>
		</div>
	</div>
</dialog>

{#if data.collection}
	<div class="flex flex-row justify-between">
		<h1 class="txt-h1">Collection</h1>
		<form method="get" action="/api/collection/export" class="flex flex-row items-center gap-2">
			<button
				type="submit"
				class="inline-flex items-center rounded-md border bg-white py-2 pr-3 pl-2 enabled:border-teal-700 enabled:text-teal-700 enabled:hover:bg-teal-700 enabled:hover:text-teal-50 disabled:border-slate-300 disabled:text-slate-300"
			>
				<DownloadSimple size="1.5em" class="mr-2"></DownloadSimple>
				Export</button
			>
			<button
				type="button"
				class="inline-flex items-center rounded-md border bg-white py-2 pr-3 pl-2 enabled:border-teal-700 enabled:text-teal-700 enabled:hover:bg-teal-700 enabled:hover:text-teal-50 disabled:border-slate-300 disabled:text-slate-300"
				onclick={showDialog}
			>
				<UploadSimple size="1.5em" class="mr-2"></UploadSimple>
				Import</button
			>
		</form>
	</div>
	<div class="flex flex-row gap-4">
		<p>Unique Cards: {data.collection.info.uniqueCardCount}</p>
		<p>Total Cards: {data.collection.info.totalCardCount}</p>
		<label>
			<input type="checkbox" bind:checked={editingEnabled} />
			Allow editing
		</label>
	</div>

	{#await getSets() then sets}
		{#await getSpecies() then species}
			{#await getRarities() then rarities}
				<CardFilters search={data.search} {sets} {species} {rarities} changeCallback={runSearch} />
			{/await}
		{/await}
	{/await}
	{#if data.collection && data.collection.cardPage.content.length > 0}
		<Pagination pageInfo={data.collection.cardPage.page} path="/collection" />
		<div
			class="grid grid-cols-2 gap-2 md:grid-cols-4 md:gap-4 lg:grid-cols-5 lg:gap-8 xl:grid-cols-8"
		>
			{#each data.collection.cardPage.content as card, i (card.id)}
				<CountedCardStub
					{card}
					amount={card.amount}
					enableEdit={editingEnabled}
					onChange={(newAmount: number) => {
						amountChange(card, i, newAmount);
					}}
				/>
			{/each}
		</div>
		<Pagination pageInfo={data.collection.cardPage.page} path="/collection" />
	{:else if data.search.isDefault()}
		<h1>Your collection is empty.</h1>
	{:else}
		<h1>No results. Try adjusting the filters or collect matching cards.</h1>
	{/if}
{:else}
	<h1>NOT FOUND</h1>
{/if}
