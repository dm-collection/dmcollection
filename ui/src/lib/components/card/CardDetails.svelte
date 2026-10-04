<script lang="ts">
	import type { Printing, PrintingSide } from '$lib/types/card';
	import AmountButton from '../AmountButton.svelte';
	import SideDetails from './SideDetails.svelte';
	import { api } from '$lib/api';
	import { onDestroy } from 'svelte';
	import { createDebouncedAmountSync } from '$lib/debouncedAmountSync';

	let {
		printing: printing,
		collectionEntry
	}: { printing: Printing; collectionEntry: { printingId: number; amount: number } | undefined } =
		$props();
	let cards: Array<Array<PrintingSide>> = $state([]);
	if (printing.sides) {
		for (const [i, side] of printing.sides.entries()) {
			if (side.imageFile || i == 0) {
				cards.push([side]);
			} else {
				cards[cards.length - 1].push(side);
			}
		}
	}
	const amountSync = createDebouncedAmountSync(
		async (printingId, amount) => {
			const response = await api(`/api/collectionStub/printings/${printingId}`, {
				method: 'PUT',
				json: { amount },
				keepalive: true
			});
			if (!response.ok) {
				console.error(response.statusText);
			}
			return response.ok;
		},
		(printingId, confirmedAmount) => {
			if (printingId === printing.id) {
				collectionEntry = { printingId, amount: confirmedAmount };
			}
		}
	);
	onDestroy(amountSync.flush);

	function onChange(newAmount: number) {
		amountSync.set(printing.id, collectionEntry?.amount ?? 0, newAmount);
		collectionEntry = { printingId: printing.id, amount: newAmount };
	}
</script>

<svelte:window onpagehide={amountSync.flush} />

<div class="flex flex-col gap-4">
	<div class="flex flex-row items-center justify-evenly">
		{#if printing.idText || printing.rarity}
			<div class="flex flex-row gap-1">
				<p class="text-lg font-semibold">{printing.idText}</p>
				{#if printing.rarity}
					<span
						class="p-y-1 inline-flex items-center rounded bg-white px-2 text-xs font-medium text-black ring-1 ring-black/10 ring-inset"
						>{printing.rarity}</span
					>
				{/if}
			</div>
		{/if}
		<div class="flex flex-row items-center gap-2">
			<p>Owned:</p>
			<AmountButton value={collectionEntry?.amount ?? 0} min={0} {onChange} />
		</div>
	</div>

	{#each cards as sides (sides[0].position)}
		<div class="mx-auto flex max-w-fit flex-wrap items-stretch justify-center gap-8">
			{#if sides[0]?.imageFile}
				<img
					onload={(event) => {
						const img = event.target as HTMLImageElement;
						if (img?.naturalWidth < 400) {
							img.classList.replace('min-w-[min(400px,100%)]', 'min-w-min');
						}
					}}
					src={`/image/${sides[0].imageFile}`}
					srcset={`/image/250x0/${sides[0].imageFile} 250w, /image/650x0/${sides[0].imageFile} 650w`}
					alt="Image showing facet #{sides[0].position} of the card {printing.dmId}"
					class="max-h-screen w-auto max-w-[min(500px,100%)] min-w-[min(400px,100%)] shrink object-scale-down"
				/>
			{/if}
			<div class="max-w-prose flex-[30ch]">
				<div class={['flex', 'h-full', 'flex-col', 'justify-between', 'gap-8']}>
					{#each sides as side (side.position)}
						<SideDetails {side} />
					{/each}
				</div>
			</div>
		</div>
	{/each}
</div>
