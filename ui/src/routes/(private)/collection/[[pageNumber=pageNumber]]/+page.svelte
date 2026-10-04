<script lang="ts">
	import { goto, invalidate } from '$app/navigation';
	import CardFilters from '$lib/components/CardFilters.svelte';
	import Pagination from '$lib/components/Pagination.svelte';
	import { getRarities } from '$lib/rarity.svelte';
	import { getSets } from '$lib/sets.svelte';
	import { getSpecies } from '$lib/species.svelte';
	import type { PageProps } from './$types';
	import DownloadSimpleIcon from 'phosphor-svelte/lib/DownloadSimpleIcon';
	import UploadSimpleIcon from 'phosphor-svelte/lib/UploadSimpleIcon';
	import WarningIcon from 'phosphor-svelte/lib/WarningIcon';
	import CircleNotchIcon from 'phosphor-svelte/lib/CircleNotchIcon';
	import { api } from '$lib/api';
	import type { CardStub, PrintingStub } from '$lib/types/card';
	import CountedPrintingStub from '$lib/components/CountedPrintingStub.svelte';
	import { SvelteSet } from 'svelte/reactivity';
	import ExpandingCardStub from '$lib/components/ExpandingCardStub.svelte';
	import CaretUpIcon from 'phosphor-svelte/lib/CaretUpIcon';
	import { onDestroy } from 'svelte';
	import { createDebouncedAmountSync } from '$lib/debouncedAmountSync';

	let { data }: PageProps = $props();

	let importDialog: HTMLDialogElement;
	let importFiles: FileList | null = $state(null);
	let uploading = $state(false);
	let uploadError = $state(false);

	// svelte-ignore state_referenced_locally
	let cards = $state(data.collection?.cardPage?.content);

	// svelte-ignore state_referenced_locally
	let info = $state(data.collection?.info);

	$effect(() => {
		cards = data.collection?.cardPage?.content;
		info = data.collection?.info;
	});

	let expanded = new SvelteSet<CardStub>();

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

				const response = await api('/api/collection/import', {
					binary: fileBytes
				});
				return response.ok;
			} catch (error) {
				console.error('Error importing collection', error);
				return false;
			}
		}
	}

	async function expand(card: CardStub) {
		expanded.add(card);
	}

	async function collapse(card: CardStub) {
		expanded.delete(card);
	}

	function applyAmount(card: CardStub, printing: PrintingStub, newAmount: number) {
		const cardWasOwned = card.printings.some((p) => p.amount > 0);
		const copiesDelta = newAmount - printing.amount;
		printing.amount = newAmount;
		const cardIsOwned = card.printings.some((p) => p.amount > 0);
		if (info) {
			info.numberOfCopies += copiesDelta;
			info.numberOfCards += Number(cardIsOwned) - Number(cardWasOwned);
		}
	}

	// only used to look up printings for reverting, needs no reactivity
	// eslint-disable-next-line svelte/prefer-svelte-reactivity
	const changedPrintings = new Map<number, { card: CardStub; printing: PrintingStub }>();
	const amountSync = createDebouncedAmountSync(
		async (printingId, amount) => {
			const response = await api(`/api/collectionStub`, {
				method: 'PUT',
				json: { printingId: printingId, amount },
				keepalive: true
			});
			return response.ok;
		},
		(printingId, confirmedAmount) => {
			const changed = changedPrintings.get(printingId);
			if (changed) {
				applyAmount(changed.card, changed.printing, confirmedAmount);
			}
		}
	);
	onDestroy(amountSync.flush);

	function amountChange(card: CardStub, printing: PrintingStub, newAmount: number) {
		changedPrintings.set(printing.id, { card, printing });
		amountSync.set(printing.id, printing.amount, newAmount);
		applyAmount(card, printing, newAmount);
	}
</script>

<svelte:window onpagehide={amountSync.flush} />

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
			<WarningIcon size="2em" weight="bold" class="mr-2"></WarningIcon>
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
					<CircleNotchIcon class="animate-spin"></CircleNotchIcon>
				{/if}
				Import</button
			>
		</div>
	</div>
</dialog>

<div class="flex flex-row justify-between">
	<h1 class="txt-h1">Collection</h1>
	<form method="get" action="/api/collection/export" class="flex flex-row items-center gap-2">
		<button
			type="submit"
			class="inline-flex items-center rounded-md border bg-white py-2 pr-3 pl-2 enabled:border-teal-700 enabled:text-teal-700 enabled:hover:bg-teal-700 enabled:hover:text-teal-50 disabled:border-slate-300 disabled:text-slate-300"
		>
			<DownloadSimpleIcon size="1.5em" class="mr-2"></DownloadSimpleIcon>
			Export</button
		>
		<button
			type="button"
			class="inline-flex items-center rounded-md border bg-white py-2 pr-3 pl-2 enabled:border-teal-700 enabled:text-teal-700 enabled:hover:bg-teal-700 enabled:hover:text-teal-50 disabled:border-slate-300 disabled:text-slate-300"
			onclick={showDialog}
		>
			<UploadSimpleIcon size="1.5em" class="mr-2"></UploadSimpleIcon>
			Import</button
		>
	</form>
</div>
<div class="flex flex-row gap-4">
	<p>Cards: {info?.numberOfCards ?? 0}</p>
	<p>Copies: {info?.numberOfCopies ?? 0}</p>
</div>

{#await getSets() then sets}
	{#await getSpecies() then species}
		{#await getRarities() then rarities}
			<CardFilters search={data.search} {sets} {species} {rarities} changeCallback={runSearch} />
		{/await}
	{/await}
{/await}
{#if cards && cards.length > 0 && data.collection?.cardPage}
	<Pagination pageInfo={data.collection.cardPage.page} path="/collection" />
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
									amountChange(card, printing, newAmount);
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
							amountChange(card, printing, newAmount);
						}}
					/>
				{/each}
			{/if}
		{/each}
	</div>
	<Pagination pageInfo={data.collection.cardPage.page} path="/collection" />
{:else if data.search.isDefault()}
	<h1>Your collection is empty.</h1>
{:else}
	<h1>No results. Try adjusting the filters or collect matching cards.</h1>
{/if}
