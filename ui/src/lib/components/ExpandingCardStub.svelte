<script lang="ts">
	import type { CardStub } from '$lib/types/card';
	import type { DeckCardStub } from '$lib/types/deck';
	import CaretDownIcon from 'phosphor-svelte/lib/CaretDownIcon';

	const {
		card,
		amount,
		max,
		enforcemax = false,
		sizes = '100vw',
		onClick
	}: {
		card: CardStub | DeckCardStub;
		amount: number;
		max?: number;
		enforcemax?: boolean;
		sizes?: string;
		onClick: () => void;
	} = $props();
	const imageStack = $derived(
		card.printings
			.flatMap((p) => (p.imageFileNames.length > 0 ? p.imageFileNames[0] : []))
			.slice(0, 7)
	);
</script>

<div
	class="group flex flex-col items-center rounded-t-lg rounded-b-md border border-gray-300 bg-white pb-2 hover:drop-shadow-md active:bg-teal-50"
>
	<div class="flex flex-col items-center">
		{#if imageStack.length > 0}
			<button class="grid" onclick={onClick} aria-label="expand">
				{#each imageStack.toReversed() as imageName, i (imageName)}
					<img
						src={`/image/${imageName}`}
						srcset={`/image/250x0/${imageName} 250w, /image/650x0/${imageName} 650w`}
						{sizes}
						alt={card.name}
						class={[
							'col-start-1 row-start-1 h-full w-full',
							'rounded-md',
							'object-cover',
							i === imageStack.length - 1 && 'group-hover:opacity-90'
						]}
						style={i > 0
							? `z-index: ${i}; transform: scale(${1 - i * 0.01}); transform-origin: top left;`
							: ''}
					/>
				{/each}
			</button>
			<button class="flex flex-row items-center" onclick={onClick}
				>{card.printings.length} printings <CaretDownIcon></CaretDownIcon></button
			>
		{/if}
	</div>
	<div class="flex grow flex-col items-center justify-center">
		<p
			class={[
				'inline-flex h-7.5 w-7.5 items-center justify-center rounded-md bg-slate-50 text-lg font-medium ring-1 ring-slate-300 ring-inset md:h-8.5 md:w-8.5',
				max != undefined && amount > max && enforcemax && 'text-red-500'
			]}
		>
			{amount}{max != undefined ? `/${max}` : ''}
		</p>
	</div>
</div>
