<script lang="ts">
	import type { CardStub } from '$lib/types/card';
	import type { DeckCardStub } from '$lib/types/deck';

	const {
		card,
		amount,
		max,
		sizes = '100vw',
		onClick
	}: {
		card: CardStub | DeckCardStub;
		amount: number;
		max?: number;
		sizes?: string;
		onClick: () => void;
	} = $props();
	const printing = $derived(card.printings[0]);
</script>

<button
	class="group flex flex-col items-center rounded-t-lg rounded-b-md border border-gray-300 bg-white pb-2 hover:drop-shadow-md active:bg-teal-50"
	onclick={onClick}
>
	<div class="flex flex-col items-center">
		{#if printing.imageFileNames && printing.imageFileNames?.length > 0}
			<img
				src={`/image/${printing.imageFileNames[0]}`}
				srcset={`/image/250x0/${printing.imageFileNames[0]} 250w, /image/650x0/${printing.imageFileNames[0]} 650w`}
				{sizes}
				alt={card.name}
				class="rounded-md object-cover group-hover:opacity-90"
			/>
		{/if}
		<p class="text-base">expand {card.printings.length}</p>
	</div>
	<span
		class="inline-flex h-7.5 w-7.5 items-center justify-center rounded-md bg-slate-50 text-lg font-medium ring-1 ring-slate-300 ring-inset md:h-8.5 md:w-8.5"
		>{amount}{max != undefined ? `/${max}` : ''}</span
	>
</button>
