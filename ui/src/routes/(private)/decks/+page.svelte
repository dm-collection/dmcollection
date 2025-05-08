<script lang="ts">
	import { goto, invalidate } from '$app/navigation';
	import type { PageData } from './$types';
	import CircleNotch from 'phosphor-svelte/lib/CircleNotch';
	import DownloadSimple from 'phosphor-svelte/lib/DownloadSimple';
	import Plus from 'phosphor-svelte/lib/Plus';
	import Trash from 'phosphor-svelte/lib/Trash';
	import UploadSimple from 'phosphor-svelte/lib/UploadSimple';
	import { formatDistanceToNow, formatRFC3339 } from 'date-fns';
	import type { CollectionInfo } from '$lib/types/collection';

	let { data }: { data: PageData } = $props();

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

	async function startImport() {
		if (importFiles && importFiles.length > 0) {
			uploadError = false;
			uploading = true;
			const success = await importDecks();
			if (!success) {
				uploadError = true;
				uploading = false;
			} else {
				uploading = false;
				closeDialog();
				invalidate('/api/decks');
			}
		}
	}

	async function importDecks() {
		if (importFiles) {
			try {
				const fileBytes = await importFiles[0].arrayBuffer();

				const response = await fetch('/api/decks/import', {
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
				return response.ok;
			} catch (error) {
				console.error('Error importing from file', error);
				return false;
			}
		}
	}

	function handleDialogClose(e: Event) {
		if (uploading) {
			e.preventDefault();
		}
	}

	async function createDeck() {
		let newDeckName = 'New Deck';
		if (data.decks) {
			let deckNumber = 0;
			const names = data.decks.map((d) => d.name);
			while (names.find((n) => n == newDeckName)) {
				deckNumber++;
				newDeckName = `New Deck (${deckNumber})`;
			}
		}
		const response = await fetch('/api/decks', {
			method: 'POST',
			headers: {
				'Content-Type': 'application/json',
				'X-XSRF-TOKEN':
					document.cookie
						.split('; ')
						.find((row) => row.startsWith('XSRF-TOKEN='))
						?.split('=')[1] ?? ''
			},

			body: JSON.stringify({ name: newDeckName })
		});
		if (response.ok) {
			const newDeckInfo = (await response.json()) as CollectionInfo;
			goto(`/deck/${newDeckInfo.id}`);
		}
	}

	async function deleteDeck(id: string) {
		const response = await fetch(`api/deck/${id}`, {
			method: 'DELETE'
		});
		if (response.ok) {
			invalidate('/api/decks');
		}
	}

	async function gotoDeck(id: string) {
		await goto(`/deck/${id}`);
	}
</script>

<svelte:head>
	<title>Decks</title>
</svelte:head>

<dialog
	id="importDialog"
	class="rounded-md px-6 py-6 shadow-lg"
	class:cursor-progress={uploading}
	bind:this={importDialog}
	oncancel={handleDialogClose}
>
	<div class="flex flex-col gap-4">
		<h1 class="txt-h1">Deck import</h1>
		<div class="flex flex-col gap-2">
			<label for="import">Select a file to import from:</label>
			<input
				type="file"
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

<h1 class="txt-h1">Decks</h1>

{#if !data.decks || data.decks.length == 0}
	<h2 class="my-2 text-lg">You have no decks yet</h2>
	<div class="flex flex-row gap-2">
		<button class="btn-primary" onclick={createDeck}>
			<span>New</span>
			<Plus class="ml-2" weight="bold" size="1.5em"></Plus>
		</button>
		<button type="button" class="btn-secondary" onclick={showDialog}>
			<UploadSimple size="1.5em" class="mr-2"></UploadSimple>Import
		</button>
	</div>
{/if}

{#if data.decks && data.decks.length > 0}
	<div class="shadow-xs relative flex w-full min-w-0 flex-col rounded bg-white">
		<div class="mb-0 flex flex-row items-center justify-between rounded-t border-0 px-4 py-3">
			<div class="px-4">
				<h3 class="text-blueGray-700 text-base font-semibold">Decks</h3>
			</div>
			<div class="flex flex-row items-center gap-2 px-4">
				<button type="button" class="btn-primary" onclick={createDeck}>
					<span>New</span>
					<Plus class="ml-2" weight="bold" size="1.5em"></Plus>
				</button>
				<button type="button" class="btn-secondary" onclick={showDialog}>
					<UploadSimple size="1.5em" class="mr-2"></UploadSimple>Import
				</button>
			</div>
		</div>
		<div class="block w-full overflow-x-auto">
			<table class="w-full border-collapse items-center bg-transparent">
				<thead>
					<tr>
						<th class="tbl-header">Name</th>
						<th class="tbl-header">Unique Cards</th>
						<th class="tbl-header">Total Cards</th>
						<th class="tbl-header">Last Modified</th>
						<th class="tbl-header">
							<form method="get" action={'/api/decks/export'} class="flex flex-row gap-2">
								<button
									type="submit"
									class="inline-flex items-center rounded-md border bg-white py-2 pl-2 pr-3 enabled:border-teal-700 enabled:text-teal-700 enabled:hover:bg-teal-700 enabled:hover:text-teal-50 disabled:border-slate-300 disabled:text-slate-300"
								>
									<DownloadSimple size="1.5em" class="mr-2"></DownloadSimple>
									Export All</button
								>
							</form>
						</th>
					</tr></thead
				>
				<tbody>
					{#each data.decks as deck}
						<tr
							class="cursor-pointer hover:bg-teal-50 active:bg-teal-700 active:text-teal-50"
							onclick={() => gotoDeck(deck.id)}
						>
							<td class="tbl-row font-semibold">
								{deck.name}
							</td>
							<td class="tbl-row">{deck.uniqueCardCount}</td>
							<td class="tbl-row" class:text-red-700={deck.totalCardCount > 40}
								>{deck.totalCardCount}</td
							>
							<td class="tbl-row" title={formatRFC3339(deck.lastModified)}
								>{formatDistanceToNow(deck.lastModified, { addSuffix: true })}</td
							>
							<td class="tbl-row">
								<div class="flex flex-row gap-2">
									<form
										method="get"
										action={`/api/deck/${deck.id}/export`}
										class="flex flex-row gap-2"
									>
										<button
											type="submit"
											class="inline-flex items-center rounded-md border bg-white py-2 pl-2 pr-3 enabled:border-teal-700 enabled:text-teal-700 enabled:hover:bg-teal-700 enabled:hover:text-teal-50 disabled:border-slate-300 disabled:text-slate-300"
										>
											<DownloadSimple size="1.5em" class="mr-2"></DownloadSimple>
											Export</button
										>
									</form>
									<button
										type="button"
										onclick={(event) => {
											event.stopPropagation();
											deleteDeck(deck.id);
										}}
										class="btn-secondary"
										aria-label="Delete"
									>
										<Trash size="1.5em"><title>Delete</title></Trash>
									</button>
								</div>
							</td>
						</tr>
					{/each}
				</tbody>
			</table>
		</div>
	</div>
{/if}
