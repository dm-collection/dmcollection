<script lang="ts">
	import type { PrintingStub } from '$lib/types/card';
	import type { DeckPrintingStub } from '$lib/types/deck';
	import AmountButton from './AmountButton.svelte';

	const {
		printing,
		amount,
		max = 2 ** 31 - 1,
		showMax = false,
		enableEdit = true,
		enforceMax = true,
		sizes = '100vw',
		onChange
	}: {
		printing: PrintingStub | DeckPrintingStub;
		amount: number;
		max?: number;
		enableEdit?: boolean;
		showMax?: boolean;
		enforceMax?: boolean;
		sizes?: string;
		onChange: (newAmount: number) => void;
	} = $props();
</script>

<div
	class="group flex flex-col items-center rounded-t-lg rounded-b-md border border-gray-300 bg-white pb-2 hover:drop-shadow-md active:bg-teal-50"
>
	<a href={`/card/${printing.officialId}`} class="flex flex-col items-center">
		{#if printing.imageFileNames && printing.imageFileNames?.length > 0}
			<img
				src={`/image/${printing.imageFileNames[0]}`}
				srcset={`/image/250x0/${printing.imageFileNames[0]} 250w, /image/650x0/${printing.imageFileNames[0]} 650w`}
				{sizes}
				alt="Image showing one side of card {printing.idText}"
				class="rounded-md object-cover group-hover:opacity-90"
			/>
		{/if}
		<p class="text-base">{printing.idText}</p>
	</a>
	{#if enableEdit}
		<AmountButton value={amount} min={0} {max} {showMax} {enforceMax} {onChange} />
	{:else}
		<span
			class="inline-flex h-7.5 w-7.5 items-center justify-center rounded-md bg-slate-50 text-lg font-medium ring-1 ring-slate-300 ring-inset md:h-8.5 md:w-8.5"
			>{amount}</span
		>
	{/if}
</div>
