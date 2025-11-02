<script lang="ts">
	import type { CardStub } from '$lib/types/card';
	import AmountButton from './AmountButton.svelte';

	const {
		card,
		amount,
		max = 2 ** 31 - 1,
		showMax = false,
		enableEdit = true,
		enforceMax = true,
		onChange
	}: {
		card: CardStub;
		amount: number;
		max?: number;
		enableEdit?: boolean;
		showMax?: boolean;
		enforceMax?: boolean;
		onChange: (newAmount: number) => void;
	} = $props();
</script>

<div
	class="group flex flex-col items-center rounded-t-lg rounded-b-md border border-gray-300 bg-white pb-2 hover:drop-shadow-md active:bg-teal-50"
>
	<a href={`/card/${card.dmId}`} class="flex flex-col items-center">
		{#if card.imagePaths && card.imagePaths?.length > 0}
			<img
				src={card.imagePaths[0]}
				alt="Image showing one side of card {card.dmId}"
				class="rounded object-cover group-hover:opacity-90"
			/>
		{/if}
		<p class="text-base">{card.idText}</p>
	</a>
	{#if enableEdit}
		<AmountButton value={amount} min={0} {max} {showMax} {enforceMax} {onChange} />
	{:else}
		<span
			class="inline-flex h-8 w-8 items-center justify-center rounded-md bg-slate-50 text-lg font-medium ring-1 ring-slate-300 ring-inset"
			>{amount}</span
		>
	{/if}
</div>
